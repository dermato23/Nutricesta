import os
import json
import logging
import requests
import time
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger("nutricesta.ai")

# La API key viaja en el header 'x-goog-api-key' y NUNCA en la URL:
# si va en la URL, los mensajes de error de requests la incluyen y puede
# terminar filtrada en logs o en respuestas al cliente.
GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

prompt_template_financial = """
Eres un asistente experto en analizar recibos de compra y clasificar productos.
El texto crudo (RAW) del OCR proviene de un recibo largo de supermercado donde el escaneo a veces separa los nombres de los productos de sus precios correspondientes en bloques distintos, o lista los precios más abajo en el mismo orden que aparecen los artículos.

Tu tarea es:
1. Reconstruir mentalmente el listado de compras asociando cada producto con su precio unitario/neto real.
   - Analiza el orden de los productos en las secciones superiores y compáralo con los bloques de precios que aparecen más abajo.
   - Ten en cuenta que los valores como "Descuento 30.00 %" o "9.960-" restan del precio del producto inmediatamente anterior.
   - El precio neto de cada producto es su precio base menos los descuentos aplicables.
2. Agrupar y clasificar todos los artículos comestibles y no comestibles en los siguientes 12 grupos autorizados, sumando el gasto total neto (en pesos colombianos, COP) en cada categoría:
   - "Proteina": Proteínas animales (carnes, pollo, de res, cerdo, pescado, jamón, salchichas, atún, huevos). NO lacteos.
   - "Verduras": Verduras, hortalizas, calabacín, tomate, cebolla, habichuela, etc.
   - "Frutas": Papaya, arándanos, uva, pitahaya, banano, etc.
   - "Lacteos": Leche, queso, yogures, mantequilla, kéfir, crema.
   - "Carbohidratos": Pan, tortillas, pastas, fusilli, harina pan, arroz.
   - "Cereales": Granola, avena, cereales de desayuno.
   - "Granos": Frijol, garbanzo, lentejas.
   - "AseoPersonal": Cepillo dental, enjuague bucal, desodorante, champú, jabón corporal.
   - "AseoHogar": Detergente, bolsas de basura, lavalozas.
   - "Snacks": Papas fritas, chocolates, pasabocas, Gaseosa CocaCola Zero, snacks dulces/salados (excepto comida para mascotas).
   - "Mascotas": Alimento para perros/gatos (ej. Whiskas, Snacks Gat Temp Camaron).
   - "Grasas": Aguacate, aceites, frutos secos.

3. Extraer el valor total exacto de la factura ("TotalFactura").
4. Extraer el descuento/ahorro total de la factura ("AhorroTotal").
5. Extraer el nombre del comercio emisor (ej. "Alkosto", "Exito", "D1", "Ara", "Carulla", "Jumbo", etc.) de manera concisa y limpia bajo el campo "Comercio". Si no lo encuentras, usa "Desconocido".
6. Evaluar la salud del mercado en una escala de 1 a 10 ("PuntajeSaludable") y proveer un motivo conciso ("MotivoSaludable").
7. Estimar rigurosamente los macronutrientes totales en GRAMOS de la compra en base a los pesos impresos de los alimentos (ej. "Granola K Proteina 600g" -> 600g de producto, "Atun 170g x6" -> 1020g, etc.) y su composición promedio de macronutrientes:
   - "Proteina_g": Gramos de proteína totales aproximados.
   - "Carbohidratos_g": Gramos de carbohidratos totales aproximados.
   - "Grasas_g": Gramos de grasas totales aproximados.
   (Calcula de manera real basada en la lista de compras del recibo, no devuelvas 0).

Devuelve UNICAMENTE un objeto JSON plano con las siguientes llaves exactas:
"TotalFactura", "AhorroTotal", "PuntajeSaludable", "MotivoSaludable", "FechaFactura", "Comercio", "Proteina", "Verduras", "Frutas", "Lacteos", "Carbohidratos", "Cereales", "Granos", "AseoPersonal", "AseoHogar", "Snacks", "Mascotas", "Grasas", "Proteina_g", "Carbohidratos_g", "Grasas_g"

IMPORTANTE: Para todos los valores monetarios de las categorías y totales, escribe solo números enteros limpios, sin puntos ni comas (ej. 738894).
Para la fecha, devuélvela en formato "YYYY-MM-DD" (FechaFactura). Si no la encuentras, usa null.

Texto del recibo a analizar:
{raw_text}
"""


prompt_template_recipes = """
Eres un chef experto. Basándote en el siguiente texto de un recibo de supermercado, extrae mentalmente los ingredientes comestibles comprados.
Luego, crea 5 sugerencias de recetas cortas (para 5 días) que ayuden al usuario a aprovechar esos ingredientes exactos sin desperdicios.
Si no hay ingredientes, inventa 5 recetas saludables.
Cada receta debe tener un "day" (Ej: "Dia 1"), "title" y "description".
Además, calcula un estimado de los macronutrientes de cada receta basado en la cantidad usada y agrega: "protein_pct", "carbs_pct" y "lipids_pct" (deben ser números enteros que sumen 100).

Devuelve UNICAMENTE una lista JSON de las 5 recetas. NO uses formato markdown.
Ejemplo de salida exacta:
[
  {
    "day": "Dia 1", 
    "title": "Ensalada", 
    "description": "Prepara la ensalada.",
    "protein_pct": 20,
    "carbs_pct": 50,
    "lipids_pct": 30
  }
]

Texto del recibo:
{raw_text}
"""

def parse_receipt_with_gemini(raw_text: str):
    """
    Envía el texto crudo del OCR a Gemini y retorna una lista de diccionarios
    que representan los artículos detectados.
    """
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise Exception("API Key de Gemini no configurada. Por favor crear el archivo .env")

    try:
        url = GEMINI_URL
        headers = {'Content-Type': 'application/json', 'x-goog-api-key': api_key}
        final_prompt = prompt_template_financial.replace("{raw_text}", raw_text)
        
        data = {
            "contents": [{"parts": [{"text": final_prompt}]}],
            "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json"
            },
            "safetySettings": [
                {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
                {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"},
                {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
                {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"}
            ]
        }
        
        max_retries = 3
        response = None
        for attempt in range(max_retries):
            response = requests.post(url, headers=headers, json=data)
            if response.status_code in [429, 503]:
                if attempt < max_retries - 1:
                    logger.warning("Error %s de Gemini. Reintentando en %s segundos...", response.status_code, 2 ** attempt)
                    time.sleep(2 ** attempt)
                    continue
            response.raise_for_status()
            break
        
        result_data = response.json()
        
        if "candidates" not in result_data or not result_data["candidates"]:
            logger.warning("Respuesta inesperada de Gemini: %s", result_data)
            return []

        candidate = result_data["candidates"][0]
        finish_reason = candidate.get("finishReason", "UNKNOWN")
        logger.debug("Gemini Finish Reason: %s", finish_reason)

        if "content" not in candidate or "parts" not in candidate["content"]:
            logger.warning("La respuesta fue bloqueada o no tiene contenido: %s", candidate)
            return []

        raw_response = candidate["content"]["parts"][0]["text"].strip()
        logger.debug("Respuesta cruda de Gemini:\n%s", raw_response)
        
        # Remover bloques de markdown si los hay
        if raw_response.startswith("```json"):
            raw_response = raw_response[7:]
        elif raw_response.startswith("```"):
            raw_response = raw_response[3:]
            
        if raw_response.endswith("```"):
            raw_response = raw_response[:-3]
            
        result_json = json.loads(raw_response.strip())
        return result_json
    except Exception as e:
        logger.error("Error procesando con Gemini REST API: %s", e)
        raise e

def generate_recipes_with_gemini(raw_text: str, preferences: dict = None):
    """
    Envía el recibo a Gemini para que genere 5 recetas en formato JSON asíncronamente.
    """
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return []

    try:
        url = GEMINI_URL
        headers = {'Content-Type': 'application/json', 'x-goog-api-key': api_key}

        pref_text = ""
        if preferences:
            time_pref = preferences.get("time", "")
            diets = preferences.get("diets", [])
            allergies = preferences.get("allergies", [])
            goal = preferences.get("goal", "")
            dish_types = preferences.get("dish_types", [])
            
            pref_text = "\n\nIMPORTANTE: Adapta estrictamente las recetas a las siguientes preferencias del usuario:\n"
            if time_pref:
                pref_text += f"- Tiempo de preparación: Cada receta debe poder hacerse en menos de {time_pref}.\n"
            if diets:
                pref_text += f"- Dietas que sigue: {', '.join(diets)}.\n"
            if allergies:
                pref_text += f"- Alergias o intolerancias alimentarias (EVITA POR COMPLETO el uso de estos ingredientes): {', '.join(allergies)}.\n"
            if goal:
                pref_text += f"- Objetivo principal: {goal}.\n"
            if dish_types:
                pref_text += f"- Tipos de plato recomendados (intenta ajustarte a estos tipos si es posible): {', '.join(dish_types)}.\n"
        
        final_prompt = prompt_template_recipes.replace("{raw_text}", raw_text) + pref_text
        
        data = {
            "contents": [{"parts": [{"text": final_prompt}]}],
            "generationConfig": {
                "temperature": 0.7,
                "responseMimeType": "application/json"
            }
        }
        
        response = requests.post(url, headers=headers, json=data)
        response.raise_for_status()
        
        result_data = response.json()
        candidate = result_data["candidates"][0]
        raw_response = candidate["content"]["parts"][0]["text"].strip()
        
        return json.loads(raw_response)
    except Exception as e:
        logger.error("Error generando recetas: %s", e)
        return []

def ask_nutrition_with_gemini(raw_text: str, question: str):
    """
    Envía la pregunta del usuario y el contenido del recibo a Gemini para recibir una respuesta textual.
    """
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return "Error: API Key de Gemini no configurada."

    try:
        url = GEMINI_URL
        headers = {'Content-Type': 'application/json', 'x-goog-api-key': api_key}

        prompt = f"""
Eres un asistente experto en nutrición y compras saludables llamado NutriIA.
Un usuario te hace una pregunta y debes responderle basándote en los productos comprados en su factura de supermercado (cuyo texto crudo OCR se adjunta abajo).

PREGUNTA DEL USUARIO:
"{question}"

CONTENIDO DEL RECIBO (OCR RAW):
{raw_text}

Tu respuesta debe ser:
1. En español.
2. Clara, concisa y muy práctica, ofreciendo consejos útiles directamente aplicables a lo que el usuario compró o quiere comprar.
3. Máximo 3 o 4 párrafos cortos o viñetas para que sea fácil de leer en la pantalla del móvil.
4. Puedes usar emojis sutilmente para destacar puntos clave.
"""
        data = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.7
            }
        }
        
        response = requests.post(url, headers=headers, json=data)
        response.raise_for_status()
        
        result_data = response.json()
        candidate = result_data["candidates"][0]
        return candidate["content"]["parts"][0]["text"].strip()
    except Exception as e:
        # El detalle solo se registra en el servidor; al cliente va un mensaje
        # genérico para no filtrar información interna (URLs, credenciales, etc.)
        logger.error("Error consultando a NutriIA: %s", e)
        return "Lo siento, ocurrió un error al procesar tu consulta con NutriIA. Intenta de nuevo en unos minutos."

def analyze_pet_nutrition_with_gemini(raw_text: str, pet_type: str, breed: str, age_range: str) -> str:
    """
    Analiza la nutrición de la mascota basada en el tipo, raza, edad, y el texto OCR del mercado.
    """
    import os, requests
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return f"Recomendación para {pet_type} {breed} ({age_range}):\n- Requieren dieta alta en proteínas de calidad y balanceada.\n(Error: GEMINI_API_KEY no configurada)."

    try:
        url = GEMINI_URL
        headers = {'Content-Type': 'application/json', 'x-goog-api-key': api_key}

        prompt = f"""
Eres un veterinario y nutricionista de mascotas experto en Colombia.
Analiza la nutrición de un {pet_type} de raza {breed} y edad {age_range}.
Además, revisa el siguiente texto crudo del mercado del usuario para ver si compró algún alimento o producto para mascotas (ej: marcas como Chunky, Pedigree, Mirringo, Cat Chow, Dog Chow, Royal Canin, Pro Plan, etc. o genéricos como "comida perro").

TEXTO DEL MERCADO (OCR):
{raw_text}

Tu respuesta debe constar de dos partes claramente separadas:
1. Recomendación Nutricional detallada para esta mascota según su raza y edad en Colombia. (Sé específico con los requerimientos, ej: tendencia a sobrepeso, cuidado de articulaciones, pelaje, tamaño de la croqueta, etc. con viñetas cortas).
2. Nota de Calidad de Compra (Alerta): Revisa el texto del mercado. Si encuentras algún alimento para mascotas que consideres de baja calidad (por ejemplo, marcas comerciales económicas muy altas en sodio, harinas de subproductos o colorantes artificiales como Pedigree, Chunky, Mirringo, etc.), indícalo amigablemente advirtiendo por qué no es ideal y sugiriendo una alternativa más saludable (ej. marcas premium o recetas naturales). Si no encuentras ningún producto de mascota o si el que compró es de buena calidad, puedes omitir la alerta o decir que no se detectaron productos de baja calidad.

Escribe en español, claro y conciso. Máximo 150 palabras en total.
"""
        data = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.4
            }
        }
        
        response = requests.post(url, headers=headers, json=data, timeout=30)
        response.raise_for_status()
        
        result_data = response.json()
        candidate = result_data["candidates"][0]
        return candidate["content"]["parts"][0]["text"].strip()
    except Exception as e:
        logger.error("Error analizando nutrición de mascotas: %s", e)
        return f"Recomendación para {pet_type} {breed} ({age_range}):\n- Mantener una hidratación adecuada y dieta balanceada."


