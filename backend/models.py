from sqlalchemy import Column, Integer, String, Float, ForeignKey, DateTime
from sqlalchemy.orm import relationship
import datetime
from database import Base

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    email = Column(String, unique=True, index=True)
    savings_goal_percent = Column(Integer, default=10) # 10, 20 o 30
    
    pets = relationship("Pet", back_populates="owner")
    receipts = relationship("Receipt", back_populates="user")

class Pet(Base):
    __tablename__ = "pets"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String)
    animal_type = Column(String) # Perro / Gato
    breed = Column(String)
    age = Column(Integer)
    user_id = Column(Integer, ForeignKey("users.id"))
    
    owner = relationship("User", back_populates="pets")

class Receipt(Base):
    __tablename__ = "receipts"
    id = Column(Integer, primary_key=True, index=True)
    store_name = Column(String) # Exito, D1, Ara, Alkosto
    date_uploaded = Column(DateTime, default=datetime.datetime.utcnow)
    total_amount = Column(Float)
    total_savings = Column(Float, default=0.0)
    health_score = Column(Integer, default=0)
    health_reason = Column(String, default="")
    recipes = Column(String, default="[]")
    protein_g = Column(Float, default=0.0)
    carbs_g = Column(Float, default=0.0)
    fat_g = Column(Float, default=0.0)
    user_id = Column(Integer, ForeignKey("users.id"))
    
    user = relationship("User", back_populates="receipts")
    items = relationship("ReceiptItem", back_populates="receipt")

class ReceiptItem(Base):
    __tablename__ = "receipt_items"
    id = Column(Integer, primary_key=True, index=True)
    item_name = Column(String)
    category = Column(String) # Proteinas, Carbohidratos, Mascotas, etc.
    precio_neto = Column(Float) # Calculado (precio base - descuento)
    receipt_id = Column(Integer, ForeignKey("receipts.id"))
    
    receipt = relationship("Receipt", back_populates="items")
