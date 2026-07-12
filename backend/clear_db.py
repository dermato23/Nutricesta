import os
from database import engine, SessionLocal, Base
import models

def clear_db():
    print("Dropping all tables...")
    Base.metadata.drop_all(bind=engine)
    print("Creating all tables...")
    Base.metadata.create_all(bind=engine)
    
    db = SessionLocal()
    try:
        # Crear usuario por defecto
        # Datos ficticios: no usar nombres ni correos de personas reales en el código.
        user = models.User(
            id=1,
            name="Usuaria Demo",
            email="usuaria.demo@ejemplo.com",
            savings_goal_percent=10
        )
        db.add(user)
        
        # Crear mascotas por defecto (para pruebas de la sección nutrición/mascotas)
        pet = models.Pet(
            id=1,
            name="Michi",
            animal_type="Gato",
            breed="Criollo",
            age=2,
            user_id=1
        )
        db.add(pet)
        
        db.commit()
        print("Database cleared and seeded successfully!")
    except Exception as e:
        db.rollback()
        print(f"Error seeding database: {e}")
    finally:
        db.close()

if __name__ == "__main__":
    clear_db()
