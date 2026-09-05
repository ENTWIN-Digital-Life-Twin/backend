# ENTWIN Digital Life Twin — Backend

Backend for **ENTWIN**, a university PFE project: an intelligent life-management platform (planning, wellness, notifications, and later AI recommendations).

This repository is a Maven multi-module Spring Boot backend. Current milestones: **Authentication**, **Planning MVP**, **Wellness MVP**, and **Notification MVP**. Gateway remains a placeholder.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker and Docker Compose (for PostgreSQL and integration tests)

## Module structure

```text
digital-life-twin-backend/
├── pom.xml
├── docker-compose.yml
├── .env.example
├── common/                  # shared library placeholder
├── auth-service/            # fully implemented
├── planning-service/        # fully implemented (MVP)
├── wellness-service/        # fully implemented (MVP)
├── notification-service/    # fully implemented (MVP)
└── api-gateway/             # placeholder
```

| Module | Status |
| --- | --- |
| `auth-service` | Implemented: register, login, JWT, refresh rotation, logout, profile, password change |
| `planning-service` | Implemented: tasks, events, categories, conflicts, daily free-time planning |
| `wellness-service` | Implemented: sleep, hydration, meals, workouts, mood, health records, goals, daily/weekly summaries |
| `notification-service` | Implemented: reminders, in-app notifications, read/unread, DAILY/WEEKLY recurrence, scheduled processing |
| `api-gateway` | Empty Spring Boot module |
| `common` | Shared library placeholder |

## Start PostgreSQL

From the repository root:

```bash
docker compose up -d
```

This starts `dlt-postgres` on port `5432` with a persistent volume.

On **first** container init, `docker/postgres/init-databases.sh` also creates `dlt_planning`, `dlt_wellness`, and `dlt_notification`.

If the volume already existed before those databases were added, create them once:

```bash
docker exec -it dlt-postgres psql -U dlt -d dlt_auth -c "CREATE DATABASE dlt_planning;"
docker exec -it dlt-postgres psql -U dlt -d dlt_auth -c "CREATE DATABASE dlt_wellness;"
docker exec -it dlt-postgres psql -U dlt -d dlt_auth -c "CREATE DATABASE dlt_notification;"
```

Default local credentials (override via environment or a private `.env` file):

- Auth database: `dlt_auth`
- Planning database: `dlt_planning`
- Wellness database: `dlt_wellness`
- Notification database: `dlt_notification`
- Username: `dlt`
- Password: `dlt`

Copy the example environment file if you want to customize values:

```bash
cp .env.example .env
```

Do not commit `.env`.

## Required environment variables

| Variable | Purpose | Example |
| --- | --- | --- |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `dlt_auth` |
| `DB_USERNAME` | Database user | `dlt` |
| `DB_PASSWORD` | Database password | `changeme` |
| `JWT_SECRET` | HMAC secret, at least 32 characters | long random string |
| `JWT_ACCESS_EXPIRATION` | Access token lifetime in seconds | `3600` |
| `JWT_REFRESH_EXPIRATION` | Refresh token lifetime in seconds | `604800` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins | `http://localhost:4200` |
| `PLANNING_DB_NAME` | Planning service database | `dlt_planning` |
| `PLANNING_DEFAULT_TIMEZONE` | Temporary default TZ until auth profile sync | `Africa/Casablanca` |
| `PLANNING_DAY_START` | Usable day start (local) | `08:00` |
| `PLANNING_DAY_END` | Usable day end (local) | `23:00` |
| `WELLNESS_DB_NAME` | Wellness service database | `dlt_wellness` |
| `WELLNESS_DEFAULT_TIMEZONE` | Temporary default TZ until auth profile sync | `Africa/Casablanca` |
| `WELLNESS_DEFAULT_WATER_GOAL_ML` | Fallback daily water goal (auth prefs not queried) | `2000` |
| `NOTIFICATION_DB_NAME` | Notification service database | `dlt_notification` |
| `NOTIFICATION_SCHEDULER_FIXED_DELAY_MS` | Delay between reminder processing runs | `60000` |

`application.yml` includes local-development fallbacks so the service can start without a `.env` file. Override `JWT_SECRET` and database credentials before any shared or production use.

## Run auth-service

A Maven Wrapper is included (`mvnw` / `mvnw.cmd`), so a global Maven install is optional.

```bash
# from repository root
./mvnw -pl auth-service -am spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -pl auth-service -am spring-boot:run
```

If you prefer not to use `-pl`, run from the module folder:

```powershell
cd auth-service
..\mvnw.cmd spring-boot:run
```

PostgreSQL must already be running (`docker compose up -d` from the repository root).

The service listens on **http://localhost:8081** by default.

## Useful URLs

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs
- Health: http://localhost:8081/actuator/health
- Info: http://localhost:8081/actuator/info

Swagger accepts a Bearer JWT for authenticated endpoints. Authenticate via `/api/auth/login`, then use **Authorize** in Swagger UI.

## Auth API

| Method | Path | Access |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Public |
| `POST` | `/api/auth/login` | Public |
| `POST` | `/api/auth/refresh` | Public |
| `POST` | `/api/auth/logout` | Authenticated |
| `GET` | `/api/users/me` | Authenticated |
| `PUT` | `/api/users/me` | Authenticated |
| `PUT` | `/api/users/me/password` | Authenticated |

The authenticated user's ID is always taken from the JWT, never from a client-supplied path or body field.

## API versioning decision

New service endpoints should use versioned paths: `/api/v1/...`.

Existing authentication routes stay unversioned for compatibility:

- `/api/auth/register`, `/login`, `/refresh`, `/logout`
- `/api/users/me`, `/api/users/me/password`

Do not rename these to `/api/v1/...` unless a coordinated client migration is explicitly requested.

## Run planning-service

PostgreSQL must include `dlt_planning`. Use the same `JWT_SECRET` as auth-service.

```powershell
.\mvnw.cmd -pl planning-service -am spring-boot:run
```

Planning listens on **http://localhost:8082**.

- Swagger UI: http://localhost:8082/swagger-ui.html
- Health: http://localhost:8082/actuator/health

Temporary planning defaults (until auth profile timezone/preferences are integrated):

- Timezone: `PLANNING_DEFAULT_TIMEZONE` (default `Africa/Casablanca`)
- Usable day window: `PLANNING_DAY_START` → `PLANNING_DAY_END` (default `08:00` → `23:00`)

Planning validates JWTs with the shared secret and trusts claims. It does **not** query `auth_db`, so disabled/blocked status is only enforced by auth-service until the access token expires.

### Planning API (authenticated)

| Method | Path |
| --- | --- |
| `POST/GET` | `/api/v1/tasks` |
| `GET/PUT/DELETE` | `/api/v1/tasks/{id}` |
| `PATCH` | `/api/v1/tasks/{id}/status` |
| `POST/GET` | `/api/v1/events` |
| `GET/PUT/DELETE` | `/api/v1/events/{id}` |
| `GET/POST` | `/api/v1/task-categories` |
| `PUT/DELETE` | `/api/v1/task-categories/{id}` |
| `GET` | `/api/v1/planning/daily?date=YYYY-MM-DD` |

## Run wellness-service

PostgreSQL must include `dlt_wellness`. Use the same `JWT_SECRET` as auth-service.

```powershell
.\mvnw.cmd -pl wellness-service -am spring-boot:run
```

Wellness listens on **http://localhost:8083**.

- Swagger UI: http://localhost:8083/swagger-ui.html
- Health: http://localhost:8083/actuator/health

Temporary wellness defaults (until auth profile timezone/preferences are integrated):

- Timezone: `WELLNESS_DEFAULT_TIMEZONE` (default `Africa/Casablanca`)
- Daily water goal fallback: `WELLNESS_DEFAULT_WATER_GOAL_ML` (default `2000`) — does **not** read auth-service preferences

Wellness validates JWTs with the shared secret and trusts claims. It does **not** query `auth_db` or `dlt_planning`. Soft-deleted records are excluded from lists and summaries.

Sleep is assigned to the local calendar day of `wakeTime` (so last night’s sleep appears on today’s dashboard).

Hydration totals use beverage contribution factors: WATER 100%, TEA 80%, COFFEE 50%, JUICE 90%, OTHER 50%.

### Wellness API (authenticated)

| Method | Path |
| --- | --- |
| `POST/GET` | `/api/v1/wellness/sleep` |
| `GET/PUT/DELETE` | `/api/v1/wellness/sleep/{id}` |
| `POST/GET` | `/api/v1/wellness/water` |
| `GET/PUT/DELETE` | `/api/v1/wellness/water/{id}` |
| `POST/GET` | `/api/v1/wellness/meals` |
| `GET/PUT/DELETE` | `/api/v1/wellness/meals/{id}` |
| `POST/GET` | `/api/v1/wellness/workouts` |
| `GET/PUT/DELETE` | `/api/v1/wellness/workouts/{id}` |
| `POST/GET` | `/api/v1/wellness/mood` |
| `GET/PUT/DELETE` | `/api/v1/wellness/mood/{id}` |
| `POST/GET` | `/api/v1/wellness/health-records` |
| `GET/PUT/DELETE` | `/api/v1/wellness/health-records/{id}` |
| `POST/GET` | `/api/v1/wellness/goals` |
| `GET/PUT/DELETE` | `/api/v1/wellness/goals/{id}` |
| `PATCH` | `/api/v1/wellness/goals/{id}/status` |
| `GET` | `/api/v1/wellness/summary/daily?date=YYYY-MM-DD` |
| `GET` | `/api/v1/wellness/summary/weekly?startDate=YYYY-MM-DD` |

## Run notification-service

PostgreSQL must include `dlt_notification`. Use the same `JWT_SECRET` as auth-service.

```powershell
.\mvnw.cmd -pl notification-service -am spring-boot:run
```

Notification listens on **http://localhost:8084**.

- Swagger UI: http://localhost:8084/swagger-ui.html
- Health: http://localhost:8084/actuator/health

Notification validates JWTs with the shared secret and trusts claims. It does **not** query `auth_db`, `dlt_planning`, or `dlt_wellness`.

Reminder execution is scheduler-driven and creates **IN_APP** notifications only for this MVP. EMAIL/PUSH are enum values only. Recurrence is DAILY (+1 day) or WEEKLY (+7 days) in UTC. Soft-deleted reminders and notifications are excluded from APIs and scheduling.

Duplicate execution is prevented by locking the reminder row and updating `nextTriggerAt` (or disabling one-time reminders) in the same transaction as the SENT notification.

Notification templates were skipped for this MVP; reminder title/message are stored and delivered as-is.

### Notification API (authenticated)

| Method | Path |
| --- | --- |
| `POST/GET` | `/api/v1/reminders` |
| `GET/PUT/DELETE` | `/api/v1/reminders/{id}` |
| `PATCH` | `/api/v1/reminders/{id}/enabled` |
| `GET` | `/api/v1/notifications` |
| `GET` | `/api/v1/notifications/{id}` |
| `PATCH` | `/api/v1/notifications/{id}/read` |
| `PATCH` | `/api/v1/notifications/read-all` |
| `GET` | `/api/v1/notifications/unread-count` |
| `DELETE` | `/api/v1/notifications/{id}` |

## Run tests

Unit tests use JUnit 5 and Mockito. Integration tests use Testcontainers PostgreSQL, so Docker must be running.

```bash
# whole backend
./mvnw test

# auth-service only
./mvnw -pl auth-service -am test

# planning-service only
./mvnw -pl planning-service -am test

# wellness-service only
./mvnw -pl wellness-service -am test

# notification-service only
./mvnw -pl notification-service -am test

# all implemented services
./mvnw -pl auth-service,planning-service,wellness-service,notification-service -am test
```

On Windows PowerShell use `.\mvnw.cmd` instead of `./mvnw`. Integration tests need Docker Desktop running; they are skipped automatically if Docker is unavailable.

## Build

```bash
./mvnw -pl auth-service,planning-service,wellness-service,notification-service -am package
```

Auth-service image (build context is the repository root):

```bash
docker build -f auth-service/Dockerfile -t entwin-auth-service .
```

## Remaining TODOs

- Integrate user timezone/availability from auth-service into planning and wellness
- Sync daily water goal from auth-service preferences into wellness (event/API)
- EMAIL/PUSH delivery, RabbitMQ reminder events from Planning/Wellness/AI
- Introduce Spring Cloud Gateway in `api-gateway`
- Add the Python FastAPI AI service and RabbitMQ integration
- Add GitHub Actions, AWS EC2 deployment, Prometheus, and Grafana
- Email verification and password-reset flows
- User preference management API
- AI schedule optimization, traffic, recurring occurrence expansion
