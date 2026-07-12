import os
import sys
import datetime
import requests
import json
import pypdf
from io import BytesIO
from sqlalchemy.orm import Session
from dotenv import load_dotenv

# Asegurar que importaciones de base de datos funcionen
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
import database
import models
import ai_parser

load_dotenv()

SPANISH_MONTH_SHORT = {
    1: "ene", 2: "feb", 3: "mar", 4: "abr", 5: "may", 6: "jun",
    7: "jul", 8: "ago", 9: "sep", 10: "oct", 11: "nov", 12: "dic"
}

def get_most_recent_sipsa_interval(ref_date=None):
    if ref_date is None:
        ref_date = datetime.date.today()
    
    # Restar días para llegar al viernes más reciente
    days_to_subtract = (ref_date.weekday() - 4) % 7
    friday = ref_date - datetime.timedelta(days=days_to_subtract)
    saturday = friday - datetime.timedelta(days=6)
    return saturday, friday

def download_and_parse_sipsa(ref_date=None) -> dict:
    saturday, friday = get_most_recent_sipsa_interval(ref_date)
    
    # Formatear el nombre del archivo PDF según el patrón del DANE
    start_str = f"{saturday.day:02d}{SPANISH_MONTH_SHORT[saturday.month]}"
    end_str = f"{friday.day:02d}{SPANISH_MONTH_SHORT[friday.month]}"
    filename = f"bol-SIPSASemanal-{start_str}{end_str}-{friday.year}.pdf"
    url = f"https://www.dane.gov.co/files/operaciones/SIPSA/{filename}"
    
    print(f"Constructed SIPSA URL: {url}")
    
    try:
        response = requests.get(url, timeout=30)
        if response.status_code != 200:
            print(f"Error {response.status_code} downloading {url}")
            return {}
            
        reader = pypdf.PdfReader(BytesIO(response.content))
        
        # Buscar la página de tendencias
        page_text = ""
        found_page_num = -1
        for idx, page in enumerate(reader.pages):
            temp_text = page.extract_text()
            if "LO QUE MÁS SUBE" in temp_text and "LO QUE MÁS BAJA" in temp_text:
                page_text = temp_text
                found_page_num = idx + 1
                break
                
        if not page_text:
            print("Could not find Page containing Sube/Baja trends in PDF.")
            # Fallback a página 8
            if len(reader.pages) >= 8:
                print("Falling back to Page 8 index 7...")
                page_text = reader.pages[7].extract_text()
                found_page_num = 8
                
        if not page_text:
            return {}
            
        print(f"Found trend content on page {found_page_num}. Extracting structured data using Gemini...")
        
        # Consultar a Gemini para estructurar las listas de Bogotá
        api_key = os.getenv("GEMINI_API_KEY")
        if not api_key:
            raise Exception("GEMINI_API_KEY not configured.")
            
        gemini_url = ai_parser.GEMINI_URL
        prompt = f"""
Analiza el siguiente extracto de texto del Boletín SIPSA del DANE. Tu tarea es extraer la lista exacta de productos de "LO QUE MÁS SUBE" y "LO QUE MÁS BAJA" únicamente para la ciudad de "Bogotá D. C.".

Reglas de salida:
- Retorna un objeto JSON con dos listas llamadas "suben" y "bajan" que contienen los nombres simples de los alimentos.
- No agregues explicaciones, notas, ni marcas de markdown adicionales (retorna solo el JSON puro).

TEXTO DEL BOLETÍN:
{page_text}
"""
        headers = {'Content-Type': 'application/json', 'x-goog-api-key': api_key}
        data = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json"
            }
        }

        gemini_resp = requests.post(gemini_url, headers=headers, json=data, timeout=30)
        gemini_resp.raise_for_status()
        
        result_json = gemini_resp.json()
        candidate = result_json["candidates"][0]
        raw_text_extracted = candidate["content"]["parts"][0]["text"].strip()
        
        extracted_data = json.loads(raw_text_extracted)
        extracted_data["week_start"] = saturday.strftime("%Y-%m-%d")
        extracted_data["week_end"] = friday.strftime("%Y-%m-%d")
        
        return extracted_data
        
    except Exception as e:
        print(f"Error processing SIPSA PDF: {e}")
        return {}

def update_sipsa_db(db: Session, ref_date=None) -> bool:
    saturday, friday = get_most_recent_sipsa_interval(ref_date)
    week_start_str = saturday.strftime("%Y-%m-%d")
    week_end_str = friday.strftime("%Y-%m-%d")
    
    # Verificar si ya existe el registro de esta semana
    existing = db.query(models.WholesaleTrend).filter(
        models.WholesaleTrend.week_start == week_start_str,
        models.WholesaleTrend.week_end == week_end_str
    ).first()
    
    if existing:
        print(f"SIPSA records for week {week_start_str} to {week_end_str} already exist in database.")
        return True
        
    print(f"Fetching weekly SIPSA trend data for week: {week_start_str} to {week_end_str}")
    data = download_and_parse_sipsa(ref_date)
    
    if not data or "suben" not in data or "bajan" not in data:
        print("Failed to download or extract SIPSA trends.")
        return False
        
    new_trend = models.WholesaleTrend(
        week_start=data["week_start"],
        week_end=data["week_end"],
        suben=json.dumps(data["suben"], ensure_ascii=False),
        bajan=json.dumps(data["bajan"], ensure_ascii=False)
    )
    db.add(new_trend)
    db.commit()
    print("Wholesale trends updated successfully in database!")
    return True

if __name__ == "__main__":
    db = database.SessionLocal()
    try:
        update_sipsa_db(db)
    finally:
        db.close()
