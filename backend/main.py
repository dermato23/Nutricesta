from fastapi import FastAPI, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
import models, schemas, database, ai_parser
import json

models.Base.metadata.create_all(bind=database.engine)

app = FastAPI(title="Consumo Inteligente API")

# Dependencia para obtener la DB
def get_db():
    db = database.SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Categorías definidas:
# 1. Proteínas
# 2. Carbohidratos y Despensa
# 3. Frutas y Verduras
# 4. Snacks y Bebidas
# 5. Aseo y Hogar
# 6. Cuidado Personal
# 7. Mascotas

def mock_classify_and_calculate_net(raw_text: str):
    """
    Mock de lógica de clasificación. 
    En producción esto analizaría el raw_text usando regex o LLM.
    Simulamos que detecta un artículo con descuento y otro normal.
    """
    # Ejemplo de procesamiento:
    items = []
    if "POLLO ENTERO" in raw_text.upper():
        items.append({"item": "Pollo Entero Exito", "precio_neto": 15000.0, "category": "Proteínas"})
    if "DETERGENTE" in raw_text.upper():
        items.append({"item": "Detergente Ariel", "precio_neto": 20000.0, "category": "Aseo y Hogar"})
    
    # Simulación por defecto si no detecta
    if not items:
        items = [
            {"item": "Atun Lata", "precio_neto": 4500.0, "category": "Proteínas"},
            {"item": "Arroz Diana 1kg", "precio_neto": 4000.0, "category": "Carbohidratos y Despensa"},
            # Simulación de regla de negocio: precio 12000, descuento -2000 -> neto 10000
            {"item": "Shampoo Pantene", "precio_neto": 10000.0, "category": "Cuidado Personal"} 
        ]
        
    return items

def generate_recipes_background(receipt_id: int, raw_text: str):
    """
    Tarea en segundo plano que genera recetas y actualiza la base de datos.
    Se crea una nueva sesión de DB para evitar problemas de concurrencia.
    """
    db = database.SessionLocal()
    try:
        user = db.query(models.User).filter(models.User.id == 1).first()
        preferences = {}
        if user and user.recipe_preferences:
            try:
                preferences = json.loads(user.recipe_preferences)
            except Exception as e:
                print(f"Error parseando preferencias: {e}")
                
        recipes_list = ai_parser.generate_recipes_with_gemini(raw_text, preferences)
        if recipes_list:
            import json
            recipes_json = json.dumps(recipes_list, ensure_ascii=False)
            db_receipt = db.query(models.Receipt).filter(models.Receipt.id == receipt_id).first()
            if db_receipt:
                db_receipt.recipes = recipes_json
                db.commit()
    except Exception as e:
        print(f"Error en background task de recetas: {e}")
    finally:
        db.close()

@app.post("/api/v1/receipts/upload", response_model=schemas.Receipt)
def upload_receipt(request: schemas.OCRRequest, background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    """
    Recibe el texto raw procesado por el OCR en el móvil y lo clasifica usando Gemini.
    """
    print(f"--- RAW OCR TEXT RECEIVED ---\n{request.raw_text}\n-----------------------------")
    # 1. Clasificar usando la IA (Gemini)
    try:
        processed_items_dict = ai_parser.parse_receipt_with_gemini(request.raw_text)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error procesando la IA: {str(e)}")
        
    # Extraer el total y eliminarlo del diccionario para que no sea tratado como un ítem de categoría
    total = float(processed_items_dict.pop("TotalFactura", 0.0))
    if total == 0.0:
        total = sum(float(val) for val in processed_items_dict.values())
        
    ahorro = float(processed_items_dict.pop("AhorroTotal", 0.0))
    health_score = int(processed_items_dict.pop("PuntajeSaludable", 0))
    health_reason = processed_items_dict.pop("MotivoSaludable", "")
    
    store_name = processed_items_dict.pop("Comercio", "Desconocido")
    
    protein_g = 0.0
    try:
        if "Proteina_g" in processed_items_dict:
            protein_g = float(processed_items_dict.pop("Proteina_g"))
    except Exception as e:
        print(f"Error parsing Proteina_g: {e}")
        
    carbs_g = 0.0
    try:
        if "Carbohidratos_g" in processed_items_dict:
            carbs_g = float(processed_items_dict.pop("Carbohidratos_g"))
    except Exception as e:
        print(f"Error parsing Carbohidratos_g: {e}")
        
    fat_g = 0.0
    try:
        if "Grasas_g" in processed_items_dict:
            fat_g = float(processed_items_dict.pop("Grasas_g"))
    except Exception as e:
        print(f"Error parsing Grasas_g: {e}")
    
    # Extraer y parsear fecha de la factura
    fecha_factura_str = processed_items_dict.pop("FechaFactura", None)
    import datetime
    parsed_date = None
    if fecha_factura_str:
        try:
            parsed_date = datetime.datetime.strptime(fecha_factura_str.strip(), "%Y-%m-%d")
        except Exception as e:
            print(f"Error parseando fecha '{fecha_factura_str}': {e}")
            parsed_date = None
            
    if not parsed_date:
        parsed_date = datetime.datetime.utcnow()
    
    # 2. Guardar factura
    db_receipt = models.Receipt(
        store_name=store_name,
        date_uploaded=parsed_date,
        total_amount=total,
        total_savings=ahorro,
        health_score=health_score,
        health_reason=health_reason,
        recipes="[]",
        raw_text=request.raw_text,
        protein_g=protein_g,
        carbs_g=carbs_g,
        fat_g=fat_g,
        user_id=1 # Usuario dummy para demo
    )
    db.add(db_receipt)
    db.commit()
    db.refresh(db_receipt)
    
    # 3. Guardar items
    CATEGORY_LABELS = {
        "Proteina": "Proteína",
        "Verduras": "Verduras y Hortalizas",
        "Frutas": "Frutas",
        "Lacteos": "Lácteos",
        "Carbohidratos": "Carbohidratos",
        "Cereales": "Cereales",
        "Granos": "Granos",
        "AseoPersonal": "Productos de Aseo Personal",
        "AseoHogar": "Aseo de Hogar",
        "Snacks": "Snacks o Antojos",
        "Mascotas": "Mascotas",
        "Grasas": "Grasas o Lípidos"
    }

    for cat_key, val in processed_items_dict.items():
        try:
            val_num = float(val)
        except (ValueError, TypeError):
            val_num = 0.0
        if val_num > 0:
            full_category = CATEGORY_LABELS.get(cat_key, cat_key)
            db_item = models.ReceiptItem(
                item_name=f"Gastos en {full_category}",
                category=full_category,
                precio_neto=val_num,
                receipt_id=db_receipt.id
            )
            db.add(db_item)
    db.commit()
    db.refresh(db_receipt)
    
    # 4. Enviar generación de recetas a segundo plano
    background_tasks.add_task(generate_recipes_background, db_receipt.id, request.raw_text)
    
    return db_receipt

@app.get("/api/v1/stats/financial")
def get_financial_stats(year: int = None, month: int = None, db: Session = Depends(get_db)):
    from sqlalchemy import func
    import datetime
    
    # Nombres de meses en español
    MONTH_NAMES = {
        1: "Enero", 2: "Febrero", 3: "Marzo", 4: "Abril",
        5: "Mayo", 6: "Junio", 7: "Julio", 8: "Agosto",
        9: "Septiembre", 10: "Octubre", 11: "Noviembre", 12: "Diciembre"
    }
    
    latest_receipt = None
    if year is not None and month is not None:
        target_year = year
        target_month = month
        target_year_str = f"{target_year:04d}"
        target_month_str = f"{target_month:02d}"
        latest_receipt = db.query(models.Receipt).filter(
            models.Receipt.user_id == 1,
            func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str,
            func.strftime('%m', models.Receipt.date_uploaded) == target_month_str
        ).order_by(models.Receipt.date_uploaded.desc(), models.Receipt.id.desc()).first()
    else:
        latest_receipt = db.query(models.Receipt).filter(models.Receipt.user_id == 1).order_by(models.Receipt.date_uploaded.desc()).first()
        if latest_receipt:
            target_year = latest_receipt.date_uploaded.year
            target_month = latest_receipt.date_uploaded.month
        else:
            now = datetime.datetime.utcnow()
            target_year = now.year
            target_month = now.month
            
    target_year_str = f"{target_year:04d}"
    target_month_str = f"{target_month:02d}"
    month_name = MONTH_NAMES.get(target_month, "Desconocido")
    
    latest_health_score = latest_receipt.health_score if latest_receipt else 0
    latest_health_reason = latest_receipt.health_reason if latest_receipt else ""
    
    # Calcular totales del mes objetivo
    monthly_total = db.query(func.sum(models.Receipt.total_amount)).filter(
        models.Receipt.user_id == 1,
        func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str,
        func.strftime('%m', models.Receipt.date_uploaded) == target_month_str
    ).scalar() or 0.0
    
    monthly_savings = db.query(func.sum(models.Receipt.total_savings)).filter(
        models.Receipt.user_id == 1,
        func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str,
        func.strftime('%m', models.Receipt.date_uploaded) == target_month_str
    ).scalar() or 0.0
    
    yearly_savings = db.query(func.sum(models.Receipt.total_savings)).filter(
        models.Receipt.user_id == 1,
        func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str
    ).scalar() or 0.0

    # Calcular total de gastos del año
    yearly_total = db.query(func.sum(models.Receipt.total_amount)).filter(
        models.Receipt.user_id == 1,
        func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str
    ).scalar() or 0.0

    # Calcular total de gastos de la semana real (semana actual de hoy)
    now_today = datetime.datetime.utcnow()
    current_year_str = f"{now_today.year:04d}"
    current_week_str = now_today.strftime('%W')
    weekly_total = db.query(func.sum(models.Receipt.total_amount)).filter(
        models.Receipt.user_id == 1,
        func.strftime('%Y', models.Receipt.date_uploaded) == current_year_str,
        func.strftime('%W', models.Receipt.date_uploaded) == current_week_str
    ).scalar() or 0.0

    # Calcular comparación mensual y score diff
    health_score_diff = "+0"
    if latest_receipt:
        prev_receipt = db.query(models.Receipt).filter(
            models.Receipt.user_id == 1,
            (models.Receipt.date_uploaded < latest_receipt.date_uploaded) | 
            ((models.Receipt.date_uploaded == latest_receipt.date_uploaded) & (models.Receipt.id < latest_receipt.id))
        ).order_by(models.Receipt.date_uploaded.desc(), models.Receipt.id.desc()).first()
        
        if prev_receipt:
            diff_score = latest_receipt.health_score - prev_receipt.health_score
            health_score_diff = f"+{diff_score}" if diff_score >= 0 else f"{diff_score}"
            
            if prev_receipt.total_amount > 0:
                diff_percent = ((latest_receipt.total_amount - prev_receipt.total_amount) / prev_receipt.total_amount) * 100
                direction = "más" if diff_percent >= 0 else "menos"
                month_diff_text = f"{abs(diff_percent):.1f}% {direction} en el mes de {MONTH_NAMES.get(target_month, '').lower()}"
            else:
                month_diff_text = f"0% más en el mes de {MONTH_NAMES.get(target_month, '').lower()}"
        else:
            month_diff_text = f"0% más en el mes de {MONTH_NAMES.get(target_month, '').lower()}"
    else:
        month_diff_text = f"0% más en el mes de {MONTH_NAMES.get(target_month, '').lower()}"
    
    # Calcular desglose por categoría para el mes objetivo
    category_totals = db.query(
        models.ReceiptItem.category, 
        func.sum(models.ReceiptItem.precio_neto)
    ).join(models.Receipt, models.ReceiptItem.receipt_id == models.Receipt.id) \
     .filter(
        models.Receipt.user_id == 1,
        func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str,
        func.strftime('%m', models.Receipt.date_uploaded) == target_month_str
     ) \
     .group_by(models.ReceiptItem.category).all()
    
    category_dict = {cat: total for cat, total in category_totals}
    
    ALL_CATEGORIES = [
        "Proteína", "Verduras y Hortalizas", "Frutas", "Lácteos"
    ]
    
    breakdown = []
    for cat in ALL_CATEGORIES:
        cat_total = category_dict.get(cat, 0.0)
        percentage = (cat_total / monthly_total * 100) if monthly_total > 0 else 0.0
        breakdown.append({"category": cat, "percentage": round(percentage, 2), "amount": round(cat_total, 2)})
            
    # Ordenar de mayor a menor porcentaje
    breakdown.sort(key=lambda x: x["percentage"], reverse=True)
            
    # Historial de meses real (solo meses con facturas registradas, max 5)
    real_months = db.query(
        func.strftime('%Y', models.Receipt.date_uploaded).label('year'),
        func.strftime('%m', models.Receipt.date_uploaded).label('month'),
        func.sum(models.Receipt.total_amount).label('total')
    ).filter(models.Receipt.user_id == 1)\
     .group_by('year', 'month')\
     .order_by(func.strftime('%Y', models.Receipt.date_uploaded).desc(), func.strftime('%m', models.Receipt.date_uploaded).desc())\
     .limit(5).all()

    # Revertir para mostrar cronológicamente ascendente en la gráfica
    real_months.reverse()

    monthly_history = []
    for r in real_months:
        y_val = int(r.year)
        m_val = int(r.month)
        m_name_short = MONTH_NAMES.get(m_val, "")[:3]
        monthly_history.append({"month": m_name_short, "amount": round(r.total, 2)})
        
    recipes_mock = []
    if latest_receipt and latest_receipt.recipes:
        try:
            recipes_mock = json.loads(latest_receipt.recipes)
        except:
            recipes_mock = []
    
    if not recipes_mock:
        recipes_mock = [
            {"day": "Sin recetas", "title": "Sube un recibo para generar recetas", "description": "Usa la cámara para escanear tu mercado."}
        ]

    # Obtener todos los meses con facturas para el dropdown
    all_receipt_months = db.query(
        func.strftime('%Y', models.Receipt.date_uploaded).label('year'),
        func.strftime('%m', models.Receipt.date_uploaded).label('month')
    ).filter(models.Receipt.user_id == 1)\
     .group_by('year', 'month')\
     .order_by(func.strftime('%Y', models.Receipt.date_uploaded).desc(), func.strftime('%m', models.Receipt.date_uploaded).desc())\
     .all()
     
    available_months = []
    for r in all_receipt_months:
        y_val = int(r.year)
        m_val = int(r.month)
        available_months.append({
            "year": y_val,
            "month": m_val,
            "name": f"{MONTH_NAMES.get(m_val, '')} {y_val}"
        })

    # Calcular desglose por comercio para el mes objetivo
    store_totals = db.query(
        models.Receipt.store_name,
        func.sum(models.Receipt.total_amount)
    ).filter(
        models.Receipt.user_id == 1,
        func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str,
        func.strftime('%m', models.Receipt.date_uploaded) == target_month_str
    ).group_by(models.Receipt.store_name).all()
    
    store_breakdown = []
    for store, total in store_totals:
        pct = (total / monthly_total * 100) if monthly_total > 0 else 0.0
        store_breakdown.append({
            "store": store,
            "amount": round(total, 2),
            "percentage": round(pct, 2)
        })
    # Ordenar de mayor a menor gasto
    store_breakdown.sort(key=lambda x: x["amount"], reverse=True)

    return {
        "monthly_total": round(monthly_total, 2),
        "monthly_savings": round(monthly_savings, 2),
        "yearly_savings": round(yearly_savings, 2),
        "yearly_total": round(yearly_total, 2),
        "weekly_total": round(weekly_total, 2),
        "month_name": month_name,
        "month_diff_text": month_diff_text,
        "monthly_history": monthly_history,
        "latest_health_score": latest_health_score,
        "latest_health_reason": latest_health_reason,
        "categories_breakdown": breakdown,
        "store_breakdown": store_breakdown,
        "recipes": recipes_mock,
        "health_score_diff": health_score_diff,
        "protein_g": round(latest_receipt.protein_g, 2) if latest_receipt else 0.0,
        "carbs_g": round(latest_receipt.carbs_g, 2) if latest_receipt else 0.0,
        "fat_g": round(latest_receipt.fat_g, 2) if latest_receipt else 0.0,
        "available_months": available_months
    }

@app.post("/api/v1/nutrition/ask")
def ask_nutrition_question(request: schemas.AskNutritionRequest, db: Session = Depends(get_db)):
    """
    Pregunta a NutriIA sobre los productos del mercado del mes seleccionado.
    """
    from sqlalchemy import func
    import datetime
    
    print(f"--- ASK NUTRITION RECEIVED ---")
    print(f"Question: {request.question}")
    print(f"Year: {request.year} (type: {type(request.year)})")
    print(f"Month: {request.month} (type: {type(request.month)})")
    
    # Encontrar la factura más reciente del mes seleccionado
    latest_receipt = None
    if request.year is not None and request.month is not None:
        target_year_str = f"{request.year:04d}"
        target_month_str = f"{request.month:02d}"
        print(f"Searching database for Year: {target_year_str}, Month: {target_month_str}")
        latest_receipt = db.query(models.Receipt).filter(
            models.Receipt.user_id == 1,
            func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str,
            func.strftime('%m', models.Receipt.date_uploaded) == target_month_str
        ).order_by(models.Receipt.date_uploaded.desc(), models.Receipt.id.desc()).first()
        print(f"Found receipt: {latest_receipt}")
    else:
        latest_receipt = db.query(models.Receipt).filter(models.Receipt.user_id == 1).order_by(models.Receipt.date_uploaded.desc()).first()
        print(f"Fallback to latest receipt overall: {latest_receipt}")
        
    if not latest_receipt:
        return {"answer": "No tienes facturas registradas para este mes. Sube una factura primero para que pueda analizar tus compras."}
        
    raw_text = latest_receipt.raw_text if latest_receipt.raw_text else ""
    if not raw_text:
        # Construct fallback context from DB values and items
        items = db.query(models.ReceiptItem).filter(models.ReceiptItem.receipt_id == latest_receipt.id).all()
        items_str = "\n".join([f"- {item.item_name} (Categoría: {item.category}): {item.precio_neto} COP" for item in items])
        raw_text = f"""Factura de {latest_receipt.store_name} del {latest_receipt.date_uploaded.strftime('%Y-%m-%d')}
Total de la compra: {latest_receipt.total_amount} COP
Ahorro total: {latest_receipt.total_savings} COP
Macronutrientes estimulados de esta compra:
- Proteínas: {latest_receipt.protein_g}g
- Carbohidratos: {latest_receipt.carbs_g}g
- Grasas: {latest_receipt.fat_g}g
Puntaje saludable de esta compra: {latest_receipt.health_score}/10 (Evaluación: {latest_receipt.health_reason})

Artículos comprados:
{items_str}"""
        print("Fallback context constructed successfully:\n", raw_text)
        
    answer = ai_parser.ask_nutrition_with_gemini(raw_text, request.question)
    return {"answer": answer}

@app.get("/api/v1/stats/wholesale")
def get_wholesale_trends(db: Session = Depends(get_db)):
    """
    Retorna las tendencias de precios mayoristas semanales.
    Si no existe la semana actual, intenta descargarla automáticamente.
    """
    import sipsa_updater
    import json
    
    try:
        sipsa_updater.update_sipsa_db(db)
    except Exception as e:
        print(f"Error checking/updating SIPSA: {e}")
        
    trend = db.query(models.WholesaleTrend).order_by(models.WholesaleTrend.date_updated.desc()).first()
    if not trend:
        return {
            "week_start": "2026-06-06",
            "week_end": "2026-06-12",
            "suben": [
                "Cebolla cabezona blanca", "Puerro", "Calabaza", "Tangelo", 
                "Mango de azúcar", "Chócolo mazorca", "Mora de Castilla", 
                "Repollo morado", "Repollo verde", "Limón común", 
                "Aguacate Hass", "Manzana nacional", "Pepino cohombro", 
                "Remolacha", "Guayaba pera", "Pera importada", 
                "Papa sabanera", "Pimentón", "Ciruela importada", 
                "Patilla baby", "Uva Isabela"
            ],
            "bajan": [
                "Maracuyá", "Papaya Paulina", "Ciruela roja", "Espinaca", 
                "Brócoli", "Cebolla junca", "Melón Cantalup", "Zanahoria", 
                "Banano bocadillo"
            ]
        }
        
    return {
        "week_start": trend.week_start,
        "week_end": trend.week_end,
        "suben": json.loads(trend.suben),
        "bajan": json.loads(trend.bajan)
    }

@app.get("/api/v1/recommendations/smart-list")
def get_smart_list(db: Session = Depends(get_db)):
    """
    Lista sugerida basada en ciclos de abastecimiento.
    """
    return {
        "suggestions": [
            {"item": "Detergente (Detectado: Ciclo de 30 días cumplido)", "category": "Aseo y Hogar"},
            {"item": "Alimento Gato (Detectado: Ciclo de 20 días cumplido)", "category": "Mascotas"}
        ],
        "context_alerts": [
            {"alert": "Posible paro camionero", "recommendation": "Sugerimos comprar granos no perecederos hoy."}
        ]
    }

@app.get("/api/v1/stats/pets")
def get_pet_stats(db: Session = Depends(get_db)):
    from sqlalchemy import func
    import datetime
    
    latest_receipt = db.query(models.Receipt).filter(models.Receipt.user_id == 1).order_by(models.Receipt.date_uploaded.desc()).first()
    if latest_receipt:
        target_year = latest_receipt.date_uploaded.year
        target_month = latest_receipt.date_uploaded.month
    else:
        now = datetime.datetime.utcnow()
        target_year = now.year
        target_month = now.month
        
    target_year_str = f"{target_year:04d}"
    target_month_str = f"{target_month:02d}"
    
    # Nombres de meses en español
    MONTH_NAMES = {
        1: "Enero", 2: "Febrero", 3: "Marzo", 4: "Abril",
        5: "Mayo", 6: "Junio", 7: "Julio", 8: "Agosto",
        9: "Septiembre", 10: "Octubre", 11: "Noviembre", 12: "Diciembre"
    }

    # Gastos de Mascotas del mes objetivo
    monthly_total = db.query(func.sum(models.ReceiptItem.precio_neto)).join(
        models.Receipt, models.ReceiptItem.receipt_id == models.Receipt.id
    ).filter(
        models.Receipt.user_id == 1,
        models.ReceiptItem.category == "Mascotas",
        func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str,
        func.strftime('%m', models.Receipt.date_uploaded) == target_month_str
    ).scalar() or 0.0

    # Gastos de Mascotas del año objetivo
    yearly_total = db.query(func.sum(models.ReceiptItem.precio_neto)).join(
        models.Receipt, models.ReceiptItem.receipt_id == models.Receipt.id
    ).filter(
        models.Receipt.user_id == 1,
        models.ReceiptItem.category == "Mascotas",
        func.strftime('%Y', models.Receipt.date_uploaded) == target_year_str
    ).scalar() or 0.0

    # Gastos de Mascotas de la semana actual (de hoy)
    now_today = datetime.datetime.utcnow()
    current_year_str = f"{now_today.year:04d}"
    current_week_str = now_today.strftime('%W')
    weekly_total = db.query(func.sum(models.ReceiptItem.precio_neto)).join(
        models.Receipt, models.ReceiptItem.receipt_id == models.Receipt.id
    ).filter(
        models.Receipt.user_id == 1,
        models.ReceiptItem.category == "Mascotas",
        func.strftime('%Y', models.Receipt.date_uploaded) == current_year_str,
        func.strftime('%W', models.Receipt.date_uploaded) == current_week_str
    ).scalar() or 0.0

    # Historial de meses real para Mascotas
    real_months = db.query(
        func.strftime('%Y', models.Receipt.date_uploaded).label('year'),
        func.strftime('%m', models.Receipt.date_uploaded).label('month'),
        func.sum(models.ReceiptItem.precio_neto).label('total')
    ).join(models.Receipt, models.ReceiptItem.receipt_id == models.Receipt.id)\
     .filter(
        models.Receipt.user_id == 1,
        models.ReceiptItem.category == "Mascotas"
     )\
     .group_by('year', 'month')\
     .order_by(func.strftime('%Y', models.Receipt.date_uploaded).desc(), func.strftime('%m', models.Receipt.date_uploaded).desc())\
     .limit(5).all()

    # Revertir para mostrar cronológicamente ascendente en la gráfica
    real_months.reverse()

    monthly_history = []
    for r in real_months:
        y_val = int(r.year)
        m_val = int(r.month)
        m_name_short = MONTH_NAMES.get(m_val, "")[:3]
        monthly_history.append({"month": m_name_short, "amount": round(r.total, 2)})

    recommendation = "Recomendación para Gato Bosque de Noruega (3 años):\n" \
                     "- Requieren dieta alta en proteínas de calidad y ácidos grasos Omega 3 y 6 para mantener su denso pelaje.\n" \
                     "- Considera comprar pasta de malta regular para evitar las bolas de pelo."
    
    return {
        "monthly_total": round(monthly_total, 2),
        "yearly_total": round(yearly_total, 2),
        "weekly_total": round(weekly_total, 2),
        "monthly_history": monthly_history,
        "recommendation": recommendation
    }

@app.get("/api/v1/users/1/preferences")
def get_user_preferences(db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == 1).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    try:
        import json
        return json.loads(user.recipe_preferences or "{}")
    except Exception as e:
        return {}

@app.post("/api/v1/users/1/preferences")
def update_user_preferences(prefs: schemas.RecipePreferences, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == 1).first()
    if not user:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    
    import json
    prefs_dict = prefs.dict()
    user.recipe_preferences = json.dumps(prefs_dict, ensure_ascii=False)
    db.commit()
    
    # Regenerar recetas basadas en la factura más reciente
    latest_receipt = db.query(models.Receipt).filter(models.Receipt.user_id == 1).order_by(models.Receipt.date_uploaded.desc(), models.Receipt.id.desc()).first()
    if latest_receipt and latest_receipt.raw_text:
        try:
            recipes_list = ai_parser.generate_recipes_with_gemini(latest_receipt.raw_text, prefs_dict)
            if recipes_list:
                latest_receipt.recipes = json.dumps(recipes_list, ensure_ascii=False)
                db.commit()
        except Exception as e:
            print(f"Error regenerando recetas al actualizar preferencias: {e}")
            
    return {"status": "ok"}

