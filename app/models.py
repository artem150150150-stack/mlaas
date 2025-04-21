from datetime import datetime
from app import db
from werkzeug.security import generate_password_hash, check_password_hash
from flask_login import UserMixin
from app import login_manager
import pyotp
import base64
import os

@login_manager.user_loader
def load_user(user_id):
    return Admin.query.get(int(user_id))

class Hotel(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    location = db.Column(db.String(100), nullable=False)
    description = db.Column(db.Text, nullable=True)
    price_per_night = db.Column(db.Float, nullable=False)
    rooms = db.relationship('Room', backref='hotel', lazy=True, cascade="all, delete-orphan")

    def __repr__(self):
        return f"Hotel('{self.name}', '{self.location}')"

class Room(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    room_number = db.Column(db.String(10), nullable=False)
    room_type = db.Column(db.String(50), nullable=False)
    capacity = db.Column(db.Integer, nullable=False)
    is_available = db.Column(db.Boolean, default=True, nullable=False)
    hotel_id = db.Column(db.Integer, db.ForeignKey('hotel.id'), nullable=False)
    bookings = db.relationship('Booking', backref='room', lazy=True, cascade="all, delete-orphan")

    def __repr__(self):
        return f"Room('{self.room_number}', '{self.room_type}')"

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    phone = db.Column(db.String(20), nullable=True)
    bookings = db.relationship('Booking', backref='guest', lazy=True, cascade="all, delete-orphan")

    def __repr__(self):
        return f"User('{self.name}', '{self.email}')"

class Booking(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    check_in_date = db.Column(db.DateTime, nullable=False)
    check_out_date = db.Column(db.DateTime, nullable=False)
    booking_date = db.Column(db.DateTime, default=datetime.utcnow, nullable=False)
    total_price = db.Column(db.Float, nullable=False)
    is_confirmed = db.Column(db.Boolean, default=False, nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    room_id = db.Column(db.Integer, db.ForeignKey('room.id'), nullable=False)

    def __repr__(self):
        return f"Booking('{self.check_in_date}', '{self.check_out_date}')"

class Admin(db.Model, UserMixin):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(20), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(128), nullable=False)
    otp_secret = db.Column(db.String(32), nullable=True)
    otp_enabled = db.Column(db.Boolean, default=False)
    
    def set_password(self, password):
        self.password_hash = generate_password_hash(password)
        
    def check_password(self, password):
        return check_password_hash(self.password_hash, password)
    
    def get_totp_uri(self):
        """Получить URI для QR-кода TOTP"""
        if not self.otp_secret:
            self.otp_secret = base64.b32encode(os.urandom(10)).decode('utf-8')
            db.session.commit()
            
        return pyotp.totp.TOTP(self.otp_secret).provisioning_uri(
            name=self.email,
            issuer_name="Система бронирования отелей"
        )
    
    def verify_totp(self, token):
        """Проверить токен TOTP"""
        if not self.otp_secret:
            return False
        totp = pyotp.TOTP(self.otp_secret)
        return totp.verify(token)

    def __repr__(self):
        return f"Admin('{self.username}', '{self.email}')"