from app import db
import sqlite3
import os

def upgrade_database():
    """Функция для добавления полей для двухфакторной аутентификации в таблицу admin"""
    
    # Путь к файлу базы данных
    db_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'hotel.db')
    
    print(f"Попытка обновления схемы базы данных {db_path}")
    
    if not os.path.exists(db_path):
        print("Файл базы данных не найден!")
        return
    
    # Подключаемся к базе данных
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # Проверяем, существуют ли уже нужные колонки
    cursor.execute("PRAGMA table_info(admin)")
    columns = cursor.fetchall()
    column_names = [col[1] for col in columns]
    
    # Добавляем колонки, если их нет
    if 'otp_secret' not in column_names:
        print("Добавление колонки otp_secret в таблицу admin")
        cursor.execute("ALTER TABLE admin ADD COLUMN otp_secret VARCHAR(32)")
    
    if 'otp_enabled' not in column_names:
        print("Добавление колонки otp_enabled в таблицу admin")
        cursor.execute("ALTER TABLE admin ADD COLUMN otp_enabled BOOLEAN DEFAULT 0")
    
    # Сохраняем изменения
    conn.commit()
    conn.close()
    
    print("Обновление базы данных успешно завершено!")

if __name__ == "__main__":
    upgrade_database() 