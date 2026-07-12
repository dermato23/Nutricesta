from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class ReceiptItemBase(BaseModel):
    item_name: str
    precio_neto: float
    category: Optional[str] = None

class ReceiptItemCreate(ReceiptItemBase):
    pass

class ReceiptItem(ReceiptItemBase):
    id: int
    receipt_id: int
    class Config:
        from_attributes = True

class ReceiptBase(BaseModel):
    store_name: str
    total_amount: float

class ReceiptCreate(ReceiptBase):
    items: List[ReceiptItemCreate]

class Receipt(ReceiptBase):
    id: int
    date_uploaded: datetime
    user_id: int
    items: List[ReceiptItem]
    class Config:
        from_attributes = True

class OCRRequest(BaseModel):
    # El texto extraído por Google ML Kit en Android. Una factura larga ronda
    # los 8.000 caracteres; el tope evita abusos de tamaño contra la API de IA.
    raw_text: str = Field(min_length=1, max_length=20000)

class RecipePreferences(BaseModel):
    time: str
    diets: List[str]
    allergies: List[str]
    goal: str
    dish_types: List[str]

class AskNutritionRequest(BaseModel):
    question: str = Field(min_length=1, max_length=1000)
    year: Optional[int] = Field(default=None, ge=2000, le=2100)
    month: Optional[int] = Field(default=None, ge=1, le=12)

