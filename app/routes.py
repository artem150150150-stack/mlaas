from flask import render_template, render_template_string, url_for, flash, redirect, request, session, jsonify, make_response
from app import app, db
from app.models import Hotel, Room, User, Booking, Admin
from app.forms import HotelForm, RoomForm, UserForm, BookingForm, LoginForm, RegistrationForm, TOTPTokenForm, Enable2FAForm, Disable2FAForm, TOTPVerificationForm, EnableTOTPForm, DisableTOTPForm
from datetime import datetime
from flask_login import login_user, current_user, logout_user, login_required
import sqlite3 # Импорт для демонстрации уязвимости
import pyotp
import qrcode
from io import BytesIO
import base64
from PIL import Image
import io

# Главная страница
@app.route('/')
@app.route('/home')
def home():
    hotels = Hotel.query.all()
    return render_template('home.html', title='Главная', hotels=hotels)

# Аутентификация
@app.route('/register', methods=['GET', 'POST'])
def register():
    if current_user.is_authenticated:
        return redirect(url_for('home'))
    form = RegistrationForm()
    if form.validate_on_submit():
        admin = Admin(username=form.username.data, email=form.email.data)
        admin.set_password(form.password.data)
        db.session.add(admin)
        db.session.commit()
        flash('Ваша учетная запись создана! Теперь вы можете войти в систему.', 'success')
        return redirect(url_for('login'))
    return render_template('register.html', title='Регистрация', form=form)

@app.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        return redirect(url_for('home'))
    form = LoginForm()
    if form.validate_on_submit():
        admin = Admin.query.filter_by(username=form.username.data).first()
        if admin and admin.check_password(form.password.data):
            if admin.otp_enabled:
                # Если 2FA включено, сохраняем ID пользователя в сессии и перенаправляем на ввод кода
                session['admin_id'] = admin.id
                # Сохраняем next параметр, если он есть
                next_page = request.args.get('next')
                if next_page:
                    session['next'] = next_page
                return redirect(url_for('verify_totp'))
            else:
                # Если 2FA не включено, выполняем обычный вход
                login_user(admin, remember=form.remember.data)
                next_page = request.args.get('next')
                return redirect(next_page) if next_page else redirect(url_for('home'))
        else:
            flash('Ошибка входа. Проверьте имя пользователя и пароль', 'danger')
    return render_template('login.html', title='Вход', form=form)

@app.route('/verify-totp', methods=['GET', 'POST'])
def verify_totp():
    # Проверяем, есть ли ID пользователя в сессии
    if 'admin_id' not in session:
        return redirect(url_for('login'))

    form = TOTPVerificationForm()
    if form.validate_on_submit():
        admin = Admin.query.get(session['admin_id'])
        if admin and admin.verify_totp(form.token.data):
            # Выполняем вход
            login_user(admin)

            # Проверяем, есть ли сохраненная страница для перенаправления
            next_page = session.get('next')

            # Очищаем данные сессии
            session.pop('admin_id', None)
            if 'next' in session:
                session.pop('next', None)

            # Перенаправляем пользователя
            if next_page:
                return redirect(next_page)
            return redirect(url_for('home'))
        else:
            flash('Неверный код подтверждения', 'danger')

    return render_template('verify_totp.html', title='Подтверждение входа', form=form)

@app.route('/logout')
def logout():
    logout_user()
    return redirect(url_for('home'))

# Настройки двухфакторной аутентификации
@app.route('/2fa-settings', methods=['GET', 'POST'])
@login_required
def settings_2fa():
    enable_form = EnableTOTPForm()
    disable_form = DisableTOTPForm()

    if request.method == 'POST':
        if 'enable_2fa' in request.form and enable_form.validate_on_submit():
            token = enable_form.token.data
            secret = session.get('otp_secret')

            totp = pyotp.TOTP(secret)
            if totp.verify(token):
                current_user.otp_secret = secret
                current_user.otp_enabled = True
                db.session.commit()
                flash('Двухфакторная аутентификация успешно активирована!', 'success')
                return redirect(url_for('home'))
            else:
                flash('Неверный код. Пожалуйста, попробуйте снова.', 'danger')

        if 'disable_2fa' in request.form and disable_form.validate_on_submit():
            current_user.otp_secret = None
            current_user.otp_enabled = False
            db.session.commit()
            flash('Двухфакторная аутентификация отключена.', 'info')
            return redirect(url_for('home'))

    if not current_user.otp_enabled:
        # Генерируем секретный ключ
        secret = pyotp.random_base32()
        session['otp_secret'] = secret

        # Создаем QR-код
        totp = pyotp.TOTP(secret)
        uri = totp.provisioning_uri(current_user.email, issuer_name="Гостиничная система")

        qr = qrcode.QRCode(
            version=1,
            error_correction=qrcode.constants.ERROR_CORRECT_L,
            box_size=10,
            border=4,
        )
        qr.add_data(uri)
        qr.make(fit=True)

        img = qr.make_image(fill_color="black", back_color="white")
        buffered = io.BytesIO()
        img.save(buffered)
        img_str = base64.b64encode(buffered.getvalue()).decode('utf-8')
        qr_code = f"data:image/png;base64,{img_str}"

        return render_template('2fa_settings.html', title='Настройки 2FA', 
                              qr_code=qr_code, secret=secret,
                              enable_form=enable_form, disable_form=disable_form)
    else:
        return render_template('2fa_settings.html', title='Настройки 2FA', 
                              enable_form=enable_form, disable_form=disable_form)

# CRUD для отелей
@app.route('/hotels')
@login_required
def hotels():
    hotels = Hotel.query.all()
    return render_template('hotels.html', title='Отели', hotels=hotels)

@app.route('/hotel/new', methods=['GET', 'POST'])
@login_required
def new_hotel():
    form = HotelForm()
    if form.validate_on_submit():
        hotel = Hotel(name=form.name.data, location=form.location.data, 
                     description=form.description.data, price_per_night=form.price_per_night.data)
        db.session.add(hotel)
        db.session.commit()
        flash('Отель успешно добавлен!', 'success')
        return redirect(url_for('hotels'))
    return render_template('create_hotel.html', title='Новый отель', form=form, legend='Новый отель')

@app.route('/hotel/<int:hotel_id>')
@login_required
def hotel(hotel_id):
    hotel = Hotel.query.get_or_404(hotel_id)
    return render_template('hotel.html', title=hotel.name, hotel=hotel)

@app.route('/hotel/<int:hotel_id>/update', methods=['GET', 'POST'])
@login_required
def update_hotel(hotel_id):
    hotel = Hotel.query.get_or_404(hotel_id)
    form = HotelForm()
    if form.validate_on_submit():
        hotel.name = form.name.data
        hotel.location = form.location.data
        hotel.description = form.description.data
        hotel.price_per_night = form.price_per_night.data
        db.session.commit()
        flash('Информация об отеле обновлена!', 'success')
        return redirect(url_for('hotel', hotel_id=hotel.id))
    elif request.method == 'GET':
        form.name.data = hotel.name
        form.location.data = hotel.location
        form.description.data = hotel.description
        form.price_per_night.data = hotel.price_per_night
    return render_template('create_hotel.html', title='Обновить отель', 
                           form=form, legend='Обновить отель')

@app.route('/hotel/<int:hotel_id>/delete', methods=['POST'])
@login_required
def delete_hotel(hotel_id):
    hotel = Hotel.query.get_or_404(hotel_id)
    db.session.delete(hotel)
    db.session.commit()
    flash('Отель удален!', 'success')
    return redirect(url_for('hotels'))

# CRUD для комнат
@app.route('/rooms')
@login_required
def rooms():
    rooms = Room.query.all()
    return render_template('rooms.html', title='Номера', rooms=rooms)

@app.route('/room/new', methods=['GET', 'POST'])
@login_required
def new_room():
    form = RoomForm()
    form.hotel_id.choices = [(hotel.id, hotel.name) for hotel in Hotel.query.all()]
    if form.validate_on_submit():
        room = Room(room_number=form.room_number.data, room_type=form.room_type.data,
                   capacity=form.capacity.data, is_available=form.is_available.data,
                   hotel_id=form.hotel_id.data)
        db.session.add(room)
        db.session.commit()
        flash('Номер успешно добавлен!', 'success')
        return redirect(url_for('rooms'))
    return render_template('create_room.html', title='Новый номер', form=form, legend='Новый номер')

@app.route('/room/<int:room_id>')
@login_required
def room(room_id):
    room = Room.query.get_or_404(room_id)
    return render_template('room.html', title=f'Номер {room.room_number}', room=room)

@app.route('/room/<int:room_id>/update', methods=['GET', 'POST'])
@login_required
def update_room(room_id):
    room = Room.query.get_or_404(room_id)
    form = RoomForm()
    form.hotel_id.choices = [(hotel.id, hotel.name) for hotel in Hotel.query.all()]
    if form.validate_on_submit():
        room.room_number = form.room_number.data
        room.room_type = form.room_type.data
        room.capacity = form.capacity.data
        room.is_available = form.is_available.data
        room.hotel_id = form.hotel_id.data
        db.session.commit()
        flash('Информация о номере обновлена!', 'success')
        return redirect(url_for('room', room_id=room.id))
    elif request.method == 'GET':
        form.room_number.data = room.room_number
        form.room_type.data = room.room_type
        form.capacity.data = room.capacity
        form.is_available.data = room.is_available
        form.hotel_id.data = room.hotel_id
    return render_template('create_room.html', title='Обновить номер', 
                           form=form, legend='Обновить номер')

@app.route('/room/<int:room_id>/delete', methods=['POST'])
@login_required
def delete_room(room_id):
    room = Room.query.get_or_404(room_id)
    db.session.delete(room)
    db.session.commit()
    flash('Номер удален!', 'success')
    return redirect(url_for('rooms'))

# CRUD для пользователей
@app.route('/users')
@login_required
def users():
    users = User.query.all()
    return render_template('users.html', title='Пользователи', users=users)

@app.route('/user/new', methods=['GET', 'POST'])
@login_required
def new_user():
    form = UserForm()
    if form.validate_on_submit():
        user = User(name=form.name.data, email=form.email.data, phone=form.phone.data)
        db.session.add(user)
        db.session.commit()
        flash('Пользователь успешно добавлен!', 'success')
        return redirect(url_for('users'))
    return render_template('create_user.html', title='Новый пользователь', form=form, legend='Новый пользователь')

@app.route('/user/<int:user_id>')
@login_required
def user(user_id):
    user = User.query.get_or_404(user_id)
    return render_template('user.html', title=user.name, user=user)

@app.route('/user/<int:user_id>/update', methods=['GET', 'POST'])
@login_required
def update_user(user_id):
    user = User.query.get_or_404(user_id)
    form = UserForm()
    if form.validate_on_submit():
        user.name = form.name.data
        user.email = form.email.data
        user.phone = form.phone.data
        db.session.commit()
        flash('Информация о пользователе обновлена!', 'success')
        return redirect(url_for('user', user_id=user.id))
    elif request.method == 'GET':
        form.name.data = user.name
        form.email.data = user.email
        form.phone.data = user.phone
    return render_template('create_user.html', title='Обновить пользователя', 
                           form=form, legend='Обновить пользователя')

@app.route('/user/<int:user_id>/delete', methods=['POST'])
@login_required
def delete_user(user_id):
    user = User.query.get_or_404(user_id)
    db.session.delete(user)
    db.session.commit()
    flash('Пользователь удален!', 'success')
    return redirect(url_for('users'))

# CRUD для бронирований
@app.route('/bookings')
@login_required
def bookings():
    bookings = Booking.query.all()
    return render_template('bookings.html', title='Бронирования', bookings=bookings)

@app.route('/booking/new', methods=['GET', 'POST'])
@login_required
def new_booking():
    form = BookingForm()
    form.user_id.choices = [(user.id, user.name) for user in User.query.all()]
    form.room_id.choices = [(room.id, f"{room.hotel.name} - Комната {room.room_number}") 
                            for room in Room.query.filter_by(is_available=True)]
    if form.validate_on_submit():
        room = Room.query.get(form.room_id.data)
        # Расчет цены (кол-во дней * цена за ночь)
        delta = (form.check_out_date.data - form.check_in_date.data).days
        total_price = delta * room.hotel.price_per_night

        booking = Booking(
            check_in_date=form.check_in_date.data,
            check_out_date=form.check_out_date.data,
            total_price=total_price,
            is_confirmed=form.is_confirmed.data,
            user_id=form.user_id.data,
            room_id=form.room_id.data
        )

        # Если бронирование подтверждено, помечаем комнату как недоступную
        if form.is_confirmed.data:
            room.is_available = False

        db.session.add(booking)
        db.session.commit()
        flash('Бронирование успешно создано!', 'success')
        return redirect(url_for('bookings'))
    return render_template('create_booking.html', title='Новое бронирование', 
                           form=form, legend='Новое бронирование')

@app.route('/booking/<int:booking_id>')
@login_required
def booking(booking_id):
    booking = Booking.query.get_or_404(booking_id)
    return render_template('booking.html', title=f'Бронирование #{booking.id}', booking=booking)

@app.route('/booking/<int:booking_id>/update', methods=['GET', 'POST'])
@login_required
def update_booking(booking_id):
    booking = Booking.query.get_or_404(booking_id)
    form = BookingForm()
    form.user_id.choices = [(user.id, user.name) for user in User.query.all()]

    # Добавляем текущую комнату в список выбора, даже если она недоступна
    available_rooms = Room.query.filter_by(is_available=True).all()
    current_room = Room.query.get(booking.room_id)
    if current_room not in available_rooms:
        available_rooms.append(current_room)

    form.room_id.choices = [(room.id, f"{room.hotel.name} - Комната {room.room_number}") 
                            for room in available_rooms]

    if form.validate_on_submit():
        old_room = Room.query.get(booking.room_id)
        new_room = Room.query.get(form.room_id.data)

        # Освобождаем старую комнату, если выбрана новая
        if old_room.id != new_room.id:
            old_room.is_available = True

        # Рассчитываем новую цену
        delta = (form.check_out_date.data - form.check_in_date.data).days
        booking.total_price = delta * new_room.hotel.price_per_night

        booking.check_in_date = form.check_in_date.data
        booking.check_out_date = form.check_out_date.data
        booking.is_confirmed = form.is_confirmed.data
        booking.user_id = form.user_id.data
        booking.room_id = form.room_id.data

        # Обновляем статус новой комнаты
        if form.is_confirmed.data:
            new_room.is_available = False

        db.session.commit()
        flash('Информация о бронировании обновлена!', 'success')
        return redirect(url_for('booking', booking_id=booking.id))
    elif request.method == 'GET':
        form.check_in_date.data = booking.check_in_date
        form.check_out_date.data = booking.check_out_date
        form.is_confirmed.data = booking.is_confirmed
        form.user_id.data = booking.user_id
        form.room_id.data = booking.room_id
    return render_template('create_booking.html', title='Обновить бронирование', 
                           form=form, legend='Обновить бронирование')

@app.route('/booking/<int:booking_id>/delete', methods=['POST'])
@login_required
def delete_booking(booking_id):
    booking = Booking.query.get_or_404(booking_id)

    # Освобождаем комнату, если бронирование было подтверждено
    if booking.is_confirmed:
        room = Room.query.get(booking.room_id)
        room.is_available = True

    db.session.delete(booking)
    db.session.commit()
    flash('Бронирование удалено!', 'success')
    return redirect(url_for('bookings'))