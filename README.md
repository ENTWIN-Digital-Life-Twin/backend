# ENTWIN Digital Life Twin — Backend

Backend for **ENTWIN**, a university PFE project: an intelligent life-management platform (planning, wellness, notifications, and later AI recommendations).

This repository is a Maven multi-module Spring Boot backend. Current milestones: **Authentication Service** and **Planning Service MVP**. Wellness, notification, and gateway remain placeholders.

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
├── wellness-service/        # placeholder
├── notification-service/    # placeholder
└── api-gateway/             # placeholder
```

| Module | Status |
| --- | --- |
| `auth-service` | Implemented: register, login, JWT, refresh rotation, logout, profile, password change |
| `planning-service` | Implemented: tasks, events, categories, conflicts, daily free-time planning |
| `wellness-service` | Empty Spring Boot module |
| `notification-service` | Empty Spring Boot module |
| `api-gateway` | Empty Spring Boot module |
| `common` | Shared library placeholder |

## Start PostgreSQL

From the repository root:

```bash
docker compose up -d
```

This starts `dlt-postgres` on port `5432` with a persistent volume.

On **first** container init, `docker/postgres/init-databases.sh` also creates `dlt_planning`.

If the volume already existed before planning was added, create the DB once:

```bash
docker exec -it dlt-postgres psql -U dlt -d dlt_auth -c "CREATE DATABASE dlt_planning;"
```

Default local credentials (override via environment or a private `.env` file):

- Auth database: `dlt_auth`
- Planning database: `dlt_planning`
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

## Run tests

Unit tests use JUnit 5 and Mockito. Integration tests use Testcontainers PostgreSQL, so Docker must be running.

```bash
# whole backend
./mvnw test

# auth-service only
./mvnw -pl auth-service -am test

# planning-service only
./mvnw -pl planning-service -am test
```

On Windows PowerShell use `.\mvnw.cmd` instead of `./mvnw`. Integration tests need Docker Desktop running; they are skipped automatically if Docker is unavailable.

## Build

```bash
./mvnw -pl auth-service,planning-service -am package
```

Auth-service image (build context is the repository root):

```bash
docker build -f auth-service/Dockerfile -t entwin-auth-service .
```

## Remaining TODOs

- Integrate user timezone/availability from auth-service into planning
- Wellness and notification business logic
- Introduce Spring Cloud Gateway in `api-gateway`
- Add the Python FastAPI AI service and RabbitMQ integration
- Add GitHub Actions, AWS EC2 deployment, Prometheus, and Grafana
- Email verification and password-reset flows
- User preference management API
- AI schedule optimization, traffic, recurring occurrence expansion
