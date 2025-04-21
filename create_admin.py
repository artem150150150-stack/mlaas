from app import db
from app.models import Admin

def create_default_admin():
    """Создает администратора по умолчанию, если нет ни одного администратора в системе"""
    if Admin.query.count() == 0:
        admin = Admin(username='admin', email='admin@example.com')
        admin.set_password('password')
        db.session.add(admin)
        db.session.commit()
        print('Администратор по умолчанию создан')
        print('Логин: admin')
        print('Пароль: password')
    else:
        print('Администраторы уже существуют в системе')

if __name__ == '__main__':
    create_default_admin() 