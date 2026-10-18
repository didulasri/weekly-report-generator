# Weekly Report Generator & Team Dashboard â€” Backend

Spring Boot backend. See:
- [`docs/DATABASE_SCHEMA.md`](../docs/DATABASE_SCHEMA.md) â€” schema, conventions, entity rules.
- [`docs/system-design.md`](../docs/system-design.md) â€” architecture, roles, security design.
- [`docs/api-design.md`](../docs/api-design.md) â€” endpoint contracts, error format.

This step of the backend implements **authentication and authorization only**
(register, login, `/me`, JWT, role-based access control). Reports, projects,
dashboard, and admin user management are implemented in later steps.

---

## Prerequisites

- Java 17+ (JDK)
- Maven (or use the bundled `./mvnw` / `mvnw.cmd` wrapper)
- Docker (for running Postgres via `docker-compose`, and for the
  Testcontainers-based integration tests)

---

## Environment variables

Local configuration belongs in the gitignored `backend/.env`. Copy
`.env.example` to `.env` and set a strong, unique `DB_PASSWORD` and
`JWT_SECRET` before starting the app. `application.yml` imports that file
for local runs, and Docker Compose reads it when started from `backend/`.
Do not commit `.env` or use local credentials in shared or production
environments. The app defaults to `dev` locally; production must explicitly
set `SPRING_PROFILES_ACTIVE` to a non-dev profile and supply secrets through
the platform's secret store/environment.

| Variable | Local value in `.env.example` | Purpose |
| -------- | ----------------------------- | ------- |
| `DB_NAME` | `weekly_report_db` | Database name used by Compose |
| `DB_URL` | `jdbc:postgresql://localhost:5432/weekly_report_db` | JDBC connection string |
| `DB_USERNAME` | `postgres` | Local database user |
| `DB_PASSWORD` | Must be set | Local database password |
| `JWT_SECRET` | Must be set to a strong secret | HS256 signing key |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile; `dev` enables synthetic seed data |
| `JWT_EXPIRATION_MS` | `900000` | Access token lifetime, in ms |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Empty for local Mailpit | SMTP credentials, if required |
| `FRONTEND_URL` | `http://localhost:5173` | Frontend origin used in invitation/reset links |

---

## Start Postgres

```bash
cd backend
docker compose up -d
```

This starts a single `postgres:17-alpine` container on port 5432 with a
named volume. Set the local database credentials in `backend/.env`; Compose
fails if `DB_USERNAME` or `DB_PASSWORD` is missing. Keep the app's database
settings in the same file.

---

## Run migrations

Flyway runs automatically on application startup
(`spring.flyway.enabled=true`) â€” there is no separate migration command.
Starting the app (see below) against an empty database applies all
migrations in `src/main/resources/db/migration` in order. Hibernate is
configured with `spring.jpa.hibernate.ddl-auto=validate`, so the app will
refuse to start if the entities and the migrated schema disagree.

---

## Run the app

```bash
cd backend
./mvnw spring-boot:run
```

(Windows: `mvnw.cmd spring-boot:run`)

The API is available at `http://localhost:8080/api`.

---

## Run tests

```bash
cd backend
./mvnw test
```

- `JwtServiceTest` is a plain unit test (no external dependencies).
- `AuthIntegrationTest` uses Testcontainers to spin up a throwaway
  `postgres:17-alpine` container per test run â€” **Docker must be running**
  for it to pass. It boots the full Spring context, runs all Flyway
  migrations against the container, and drives the real HTTP endpoints
  through MockMvc.

---

The dev profile enables synthetic seed data for local development only.
Those sample users and passwords are public fixtures, not credentials; never
use them outside a disposable local database. Production must not activate
the dev profile.
