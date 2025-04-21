from flask_wtf import FlaskForm
from wtforms import StringField, TextAreaField, FloatField, IntegerField, SubmitField, BooleanField, DateField, SelectField, PasswordField
from wtforms.validators import DataRequired, Email, Length, NumberRange, ValidationError, EqualTo
from datetime import date
from app.models import Admin

class HotelForm(FlaskForm):
    name = StringField('Название отеля', validators=[DataRequired(), Length(min=2, max=100)])
    location = StringField('Местоположение', validators=[DataRequired(), Length(min=2, max=100)])
    description = TextAreaField('Описание')
    price_per_night = FloatField('Цена за ночь', validators=[DataRequired(), NumberRange(min=0)])
    submit = SubmitField('Сохранить')

class RoomForm(FlaskForm):
    room_number = StringField('Номер комнаты', validators=[DataRequired(), Length(min=1, max=10)])
    room_type = StringField('Тип комнаты', validators=[DataRequired(), Length(min=2, max=50)])
    capacity = IntegerField('Вместимость', validators=[DataRequired(), NumberRange(min=1)])
    is_available = BooleanField('Доступно')
    hotel_id = SelectField('Отель', coerce=int, validators=[DataRequired()])
    submit = SubmitField('Сохранить')

class UserForm(FlaskForm):
    name = StringField('ФИО', validators=[DataRequired(), Length(min=2, max=100)])
    email = StringField('Email', validators=[DataRequired(), Email()])
    phone = StringField('Телефон', validators=[Length(max=20)])
    submit = SubmitField('Сохранить')

class BookingForm(FlaskForm):
    check_in_date = DateField('Дата заезда', validators=[DataRequired()], format='%Y-%m-%d')
    check_out_date = DateField('Дата выезда', validators=[DataRequired()], format='%Y-%m-%d')
    user_id = SelectField('Гость', coerce=int, validators=[DataRequired()])
    room_id = SelectField('Номер', coerce=int, validators=[DataRequired()])
    is_confirmed = BooleanField('Подтверждено')
    submit = SubmitField('Забронировать')

    def validate_check_out_date(self, check_out_date):
        if check_out_date.data <= self.check_in_date.data:
            raise ValidationError('Дата выезда должна быть после даты заезда')
        
    def validate_check_in_date(self, check_in_date):
        if check_in_date.data < date.today():
            raise ValidationError('Дата заезда не может быть в прошлом')

class LoginForm(FlaskForm):
    username = StringField('Имя пользователя', validators=[DataRequired()])
    password = PasswordField('Пароль', validators=[DataRequired()])
    remember = BooleanField('Запомнить меня')
    submit = SubmitField('Войти')

class RegistrationForm(FlaskForm):
    username = StringField('Имя пользователя', validators=[DataRequired(), Length(min=2, max=20)])
    email = StringField('Email', validators=[DataRequired(), Email()])
    password = PasswordField('Пароль', validators=[DataRequired(), Length(min=6)])
    confirm_password = PasswordField('Подтвердите пароль', validators=[DataRequired(), EqualTo('password')])
    submit = SubmitField('Регистрация')

    def validate_username(self, username):
        admin = Admin.query.filter_by(username=username.data).first()
        if admin:
            raise ValidationError('Это имя пользователя уже занято. Пожалуйста, выберите другое.')

    def validate_email(self, email):
        admin = Admin.query.filter_by(email=email.data).first()
        if admin:
            raise ValidationError('Этот email уже зарегистрирован. Пожалуйста, выберите другой.')

# Формы для двухфакторной аутентификации
class TOTPTokenForm(FlaskForm):
    token = StringField('Введите код подтверждения', validators=[DataRequired(), Length(min=6, max=6)])
    submit = SubmitField('Подтвердить')

class Enable2FAForm(FlaskForm):
    token = StringField('Введите код из приложения', validators=[DataRequired(), Length(min=6, max=6)])
    submit = SubmitField('Включить 2FA')

class Disable2FAForm(FlaskForm):
    token = StringField('Введите код из приложения', validators=[DataRequired(), Length(min=6, max=6)])
    submit = SubmitField('Отключить 2FA')

class TOTPVerificationForm(FlaskForm):
    token = StringField('Код аутентификации', 
                        validators=[DataRequired(), Length(min=6, max=6)],
                        render_kw={"placeholder": "Введите 6-значный код"})
    submit = SubmitField('Подтвердить')

class EnableTOTPForm(FlaskForm):
    token = StringField('Код подтверждения', 
                      validators=[DataRequired(), Length(min=6, max=6)],
                      render_kw={"placeholder": "Введите 6-значный код"})
    submit = SubmitField('Активировать')

class DisableTOTPForm(FlaskForm):
    submit = SubmitField('Отключить двухфакторную аутентификацию')