import os
import json
import requests
import time
from dotenv import load_dotenv

load_dotenv()

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
5. Evaluar la salud del mercado en una escala de 1 a 10 ("PuntajeSaludable") y proveer un motivo conciso ("MotivoSaludable").
6. Estimar rigurosamente los macronutrientes totales en GRAMOS de la compra en base a los pesos impresos de los alimentos (ej. "Granola K Proteina 600g" -> 600g de producto, "Atun 170g x6" -> 1020g, etc.) y su composición promedio de macronutrientes:
   - "Proteina_g": Gramos de proteína totales aproximados.
   - "Carbohidratos_g": Gramos de carbohidratos totales aproximados.
   - "Grasas_g": Gramos de grasas totales aproximados.
   (Calcula de manera real basada en la lista de compras del recibo, no devuelvas 0).

Devuelve UNICAMENTE un objeto JSON plano con las siguientes llaves exactas:
"TotalFactura", "AhorroTotal", "PuntajeSaludable", "MotivoSaludable", "FechaFactura", "Proteina", "Verduras", "Frutas", "Lacteos", "Carbohidratos", "Cereales", "Granos", "AseoPersonal", "AseoHogar", "Snacks", "Mascotas", "Grasas", "Proteina_g", "Carbohidratos_g", "Grasas_g"

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
        url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"
        headers = {'Content-Type': 'application/json'}
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
                    print(f"Error {response.status_code} de Gemini. Reintentando en {2 ** attempt} segundos...")
                    time.sleep(2 ** attempt)
                    continue
            response.raise_for_status()
            break
        
        result_data = response.json()
        
        if "candidates" not in result_data or not result_data["candidates"]:
            print(f"Respuesta inesperada de Gemini: {result_data}")
            return []
            
        candidate = result_data["candidates"][0]
        finish_reason = candidate.get("finishReason", "UNKNOWN")
        print(f"Gemini Finish Reason: {finish_reason}")
        
        if "content" not in candidate or "parts" not in candidate["content"]:
            print(f"La respuesta fue bloqueada o no tiene contenido: {candidate}")
            return []
            
        raw_response = candidate["content"]["parts"][0]["text"].strip()
        print(f"Respuesta cruda de Gemini:\n{raw_response}")
        
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
        print(f"Error procesando con Gemini REST API: {e}")
        raise e

def generate_recipes_with_gemini(raw_text: str):
    """
    Envía el recibo a Gemini para que genere 5 recetas en formato JSON asíncronamente.
    """
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return []

    try:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"
        headers = {'Content-Type': 'application/json'}
        final_prompt = prompt_template_recipes.replace("{raw_text}", raw_text)
        
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
        print(f"Error generando recetas: {e}")
        return []
