# LumenML — MLaaS с explainability и smart optimization

Стек: **Java 21**, **Spring Boot 3**, **PostgreSQL**, **RabbitMQ**, **Smile ML**, **React + Vite + TypeScript**, **Docker Compose**.

## Быстрый старт (Docker)

```bash
cd lumenml
docker compose up --build
```

- API: `http://localhost:8080` (напрямую) или через шлюз `http://localhost:9080/api/v1/...`
- UI (Nginx SPA): внутри compose сервис `web`; единая точка **http://localhost:9080** (прокси на UI + API)
- RabbitMQ Management: `http://localhost:15672` (guest/guest)

Администратор (создаётся при старте): `admin@lumenml.dev` / `Admin123!`

## Локальная разработка

1. PostgreSQL и RabbitMQ локально или через Docker только инфраструктуру.
2. Backend: `cd backend && mvn spring-boot:run`
3. Frontend: `cd frontend && npm install && npm run dev` (прокси `/api` → `localhost:8080`)

Переменные: `JWT_SECRET`, каталог датасетов `LUMENML_STORAGE_DATASETS-DIR` (или `DATASETS_DIR` в `application.yml`).

## Пример CSV

Файл `samples/iris.csv`: целевая колонка `species`, признаки `sepal_length,sepal_width,petal_length,petal_width`, задача **CLASSIFICATION**.

## Worker

Профиль Spring `worker` включает RabbitMQ-listeners обучения и вспомогательных очередей. В Docker сервис `worker` запускается с `SPRING_PROFILES_ACTIVE=docker,worker`.

## Архитектура

- REST API `/api/v1` — JWT, роли USER/ADMIN, rate limiting, OpenAPI `/swagger-ui.html`
- Очереди: `training.jobs` (DLX → `training.jobs.dlq`), `notification.events`, `metrics.processing`
- Обучение: Smile (Random Forest, Gradient Boosting как XGBoost-подобная модель, Logistic / Linear regression), метрики, SHAP (где доступно), рекомендации

Подробнее см. исходники пакетов `com.lumenml.api`, `service`, `rabbit`, `ml`.
