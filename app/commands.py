import click
from flask.cli import with_appcontext
from app.seed import seed_database
from app.migrate_db import upgrade_database

@click.command('seed')
@with_appcontext
def seed_command():
    """Команда для заполнения базы данных тестовыми данными."""
    seed_database()

@click.command('migrate')
@with_appcontext
def migrate_command():
    """Команда для обновления схемы базы данных."""
    upgrade_database()
    
def init_commands(app):
    """Регистрация команд Flask CLI."""
    app.cli.add_command(seed_command)
    app.cli.add_command(migrate_command) 