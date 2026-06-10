from pydantic import BaseModel
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
    raw_text: str # El texto extraído por Google ML Kit en Android
