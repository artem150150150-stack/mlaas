# Система бронирования отелей

Веб-приложение на Flask для управления бронированием отелей, комнат и гостей.

## Функциональность

- Управление отелями
- Управление комнатами
- Управление пользователями
- Управление бронированиями
- Двухфакторная аутентификация (2FA) для безопасности

## Установка

1. Клонируйте репозиторий

```bash
git clone https://github.com/yourusername/hotel-booking-system.git
cd hotel-booking-system
```

2. Создайте виртуальное окружение и активируйте его

```bash
python -m venv venv
source venv/bin/activate  # Для Linux/Mac
venv\Scripts\activate     # Для Windows
```

3. Установите зависимости

```bash
pip install -r requirements.txt
```

## Запуск приложения

```bash
python run.py
```

После запуска приложение будет доступно по адресу [http://localhost:5000](http://localhost:5000)

## Заполнение базы данных тестовыми данными

```bash
flask seed
```

## Обновление схемы базы данных

```bash
flask migrate
```

## Проверка кода с помощью pylint

Проверка кода с помощью pylint:

```bash
python lint.py
```

Или выполнить проверку для конкретного файла/модуля:

```bash
pylint app/routes.py
```

## Структура проекта

```
├── app/
│   ├── __init__.py
│   ├── models.py
│   ├── routes.py
│   ├── forms.py
│   ├── commands.py
│   ├── migrate_db.py
│   ├── seed.py
│   ├── templates/
│   └── static/
├── run.py
├── create_admin.py
├── lint.py
├── requirements.txt
├── setup.cfg
└── .pylintrc
```

## Настройка двухфакторной аутентификации (2FA)

1. Войдите в систему, используя учетные данные администратора
2. Нажмите на имя пользователя в правом верхнем углу и выберите "Настройки 2FA"
3. Отсканируйте QR-код с помощью приложения аутентификации (Google Authenticator, Authy, и т.д.)
4. Введите 6-значный код из приложения и нажмите "Активировать"

## Лицензия

MIT 