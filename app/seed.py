from app import db
from app.models import Hotel, Room, Admin, User, Booking
from datetime import datetime, timedelta
from werkzeug.security import generate_password_hash
import random

def seed_database():
    """Функция для заполнения базы данных тестовыми данными."""

    # Удаление существующих данных
    Booking.query.delete()
    Room.query.delete()
    Hotel.query.delete()
    User.query.delete()

    # Проверяем, есть ли админ, если нет - создаем
    if not Admin.query.filter_by(username='admin').first():
        admin = Admin(
            username='admin',
            email='admin@example.com'
        )
        admin.set_password('admin123')
        db.session.add(admin)
        db.session.commit()
        print('Администратор по умолчанию создан:')
        print('Логин: admin')
        print('Пароль: admin123')

    # Создаем отели
    hotels = [
        Hotel(
            name='Гранд Отель',
            location='Москва',
            description='Роскошный отель в центре города с видом на Кремль',
            price_per_night=9000.0
        ),
        Hotel(
            name='Морской бриз',
            location='Сочи',
            description='Комфортабельный курортный отель на берегу моря',
            price_per_night=7500.0
        ),
        Hotel(
            name='Северное сияние',
            location='Санкт-Петербург',
            description='Уютный отель с видом на Неву и исторический центр',
            price_per_night=8200.0
        ),
        Hotel(
            name='Алтайский простор',
            location='Горно-Алтайск',
            description='Отель на природе с панорамными видами на горы',
            price_per_night=6000.0
        )
    ]

    db.session.add_all(hotels)
    db.session.commit()

    # Создаем номера для каждого отеля
    room_types = ['Стандарт', 'Делюкс', 'Люкс', 'Семейный', 'Президентский']

    for hotel in hotels:
        for i in range(1, 6):  # 5 номеров на отель
            room_type = random.choice(room_types)
            capacity = 2 if room_type == 'Стандарт' else 3 if room_type == 'Делюкс' else 4

            room = Room(
                room_number=f'{i}0{random.randint(1, 9)}',
                room_type=room_type,
                capacity=capacity,
                is_available=True,
                hotel_id=hotel.id
            )
            db.session.add(room)

    db.session.commit()

    # Создаем пользователей
    users = [
        User(name='Иванов Иван', email='ivanov@mail.ru', phone='+79001234567'),
        User(name='Петров Петр', email='petrov@mail.ru', phone='+79002345678'),
        User(name='Сидорова Анна', email='sidorova@mail.ru', phone='+79003456789'),
        User(name='Кузнецов Алексей', email='kuznetsov@mail.ru', phone='+79004567890')
    ]

    db.session.add_all(users)
    db.session.commit()

    # Создаем бронирования
    rooms = Room.query.all()
    today = datetime.utcnow().date()

    for i in range(10):  # 10 бронирований
        user = random.choice(users)
        room = random.choice(rooms)

        # Случайные даты в пределах следующих 3 месяцев
        start_days = random.randint(5, 90)
        duration = random.randint(1, 10)

        check_in = datetime.combine(today + timedelta(days=start_days), datetime.min.time())
        check_out = datetime.combine(today + timedelta(days=start_days + duration), datetime.min.time())

        # Рассчитываем стоимость
        hotel = Hotel.query.get(room.hotel_id)
        total_price = hotel.price_per_night * duration

        booking = Booking(
            check_in_date=check_in,
            check_out_date=check_out,
            booking_date=datetime.utcnow(),
            total_price=total_price,
            is_confirmed=random.choice([True, False]),
            user_id=user.id,
            room_id=room.id
        )
        db.session.add(booking)

    db.session.commit()

    print("База данных успешно заполнена тестовыми данными!")

# Функция для выполнения из консоли или из других файлов
if __name__ == "__main__":
    seed_database() 