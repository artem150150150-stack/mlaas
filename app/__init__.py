from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager

app = Flask(__name__)
app.config['SECRET_KEY'] = '123'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///hotel.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

# Инициализация расширений
db = SQLAlchemy(app)
login_manager = LoginManager(app)
login_manager.login_view = 'login'
login_manager.login_message = 'Пожалуйста, войдите для доступа к этой странице.'
login_manager.login_message_category = 'info'

from app import routes 

# Регистрация команд Flask CLI
from app.commands import init_commands
init_commands(app) 