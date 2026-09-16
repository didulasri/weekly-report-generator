# Weekly Report Generator & Team Dashboard — Backend

Spring Boot backend. See:
- [`docs/DATABASE_SCHEMA.md`](../docs/DATABASE_SCHEMA.md) — schema, conventions, entity rules.
- [`docs/system-design.md`](../docs/system-design.md) — architecture, roles, security design.
- [`docs/api-design.md`](../docs/api-design.md) — endpoint contracts, error format.

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

All secrets and environment-specific values are read from environment
variables with sane local-dev defaults baked into
[`application.yml`](src/main/resources/application.yml), so the app runs
out of the box with zero configuration.

| Variable               | Default                                                | Purpose                              |
| ----------------------- | ------------------------------------------------------ | ------------------------------------ |
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/weekly_report_db`     | JDBC connection string               |
| `DB_USERNAME`             | `postgres`                                              | Database user                        |
| `DB_PASSWORD`             | `postgres`                                              | Database password                    |
| `JWT_SECRET`              | dev-only placeholder (see `application.yml`)            | HS256 signing key — **override in any shared/deployed environment** |
| `JWT_EXPIRATION_MS`       | `86400000` (24h)                                        | Access token lifetime, in ms         |
| `CORS_ALLOWED_ORIGINS`    | `http://localhost:5173`                                 | Allowed frontend origin(s)           |
| `SPRING_PROFILES_ACTIVE`  | `dev`                                                   | Active Spring profile                |

---

## Start Postgres

```bash
cd backend
docker compose up -d
```

This starts a single `postgres:17-alpine` container on port 5432 with a
named volume, matching the `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` defaults
above. Override `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` env vars before
running `docker compose up` if you want different credentials — just make
sure the app's env vars match.

---

## Run migrations

Flyway runs automatically on application startup
(`spring.flyway.enabled=true`) — there is no separate migration command.
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
  `postgres:17-alpine` container per test run — **Docker must be running**
  for it to pass. It boots the full Spring context, runs all Flyway
  migrations against the container, and drives the real HTTP endpoints
  through MockMvc.

---

## Seeded dev logins

### Auth-step demo users (`V15__seed_auth_data.sql`)

One account per role, all with password `Password123`:

| Role        | Email               | Password      |
| ----------- | -------------------- | -------------- |
| ADMIN       | admin@example.com    | `Password123`  |
| MANAGER     | manager@example.com  | `Password123`  |
| TEAM_MEMBER | member@example.com   | `Password123`  |

### Full dataset seed users (`V14__seed_data.sql`)

A larger demo dataset (5 team members, 4 projects, 6 weeks of reports)
seeded ahead of the reports/dashboard steps. All share password
`Password123!` (note the trailing `!`, different from the auth-step users
above):

| Role        | Email                     | Password        |
| ----------- | -------------------------- | ---------------- |
| ADMIN       | admin@weeklyreport.com     | `Password123!`   |
| MANAGER     | manager@weeklyreport.com   | `Password123!`   |
| TEAM_MEMBER | member1@weeklyreport.com   | `Password123!`   |
| TEAM_MEMBER | member2@weeklyreport.com   | `Password123!`   |
| TEAM_MEMBER | member3@weeklyreport.com   | `Password123!`   |
| TEAM_MEMBER | member4@weeklyreport.com   | `Password123!`   |
| TEAM_MEMBER | member5@weeklyreport.com   | `Password123!`   |

Passwords are stored as BCrypt hashes (`BCryptPasswordEncoder`, which
accepts both `$2a$` and `$2b$` prefixes) — never in plaintext.
