from app import app, db
from app.models import Hotel, Room, User, Booking, Admin
from create_admin import create_default_admin
from app.migrate_db import upgrade_database

# Создаем контекст приложения для использования вне запросов
@app.shell_context_processor
def make_shell_context():
    return {'db': db, 'Hotel': Hotel, 'Room': Room, 'User': User, 'Booking': Booking, 'Admin': Admin}

if __name__ == '__main__':
    # Создаем/обновляем базу данных и добавляем администратора при запуске
    with app.app_context():
        # Создаем таблицы в базе данных, если их нет
        db.create_all()
        
        # Выполняем миграцию для обновления схемы базы данных
        upgrade_database()
        
        # Создаем администратора по умолчанию
        create_default_admin()
    
    # Запускаем приложение
    app.run(debug=True) 