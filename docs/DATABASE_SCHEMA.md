# Database Schema Specification

## Weekly Report Generator & Team Dashboard

This document is the single source of truth for the database design. Generate all JPA entities, enums, and Flyway migrations from this file. Do not invent tables, columns, or relationships that are not listed here.

---

## 1. Technology & Conventions

| Item               | Value                                      |
| ------------------ | ------------------------------------------ |
| Database           | PostgreSQL 17                              |
| ORM                | Spring Data JPA / Hibernate 6              |
| Migration tool     | Flyway (`src/main/resources/db/migration`) |
| Base package       | `com.weeklyreport.backend`                 |
| Entity package     | `com.weeklyreport.backend.entity`          |
| Enum package       | `com.weeklyreport.backend.entity.enums`    |
| Repository package | `com.weeklyreport.backend.repository`      |

### Naming rules

- Table names: `snake_case`, plural (`weekly_reports`, `report_tasks`)
- Column names: `snake_case`
- Java entity class names: `PascalCase`, singular (`WeeklyReport`, `ReportTask`)
- Java field names: `camelCase`

### Global JPA rules

- Every entity uses `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` on a `Long id`.
- Every `@ManyToOne` must be `fetch = FetchType.LAZY`. Never use default EAGER.
- Every `@OneToMany` on the parent side must use `cascade = CascadeType.ALL, orphanRemoval = true` for report child collections (tasks, next-week tasks, blockers, achievements, work hours). This lets a report be saved with its children in one transaction.
- All enums are persisted as `@Enumerated(EnumType.STRING)`. Never ORDINAL.
- Use Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.
- Do **not** put `@Data` or `@ToString` on entities with collections — it causes infinite recursion.
- Timestamps use `Instant`. Dates (week start/end) use `LocalDate`.
- Auditing: use `@CreatedDate` / `@LastModifiedDate` with `@EntityListeners(AuditingEntityListener.class)` and enable `@EnableJpaAuditing`.
- Never expose entities directly from controllers. Every entity needs a corresponding DTO.

### Base class

Create a `BaseEntity` mapped superclass holding `id`, `createdAt`, `updatedAt`. All entities extend it unless noted.

```
@MappedSuperclass
public abstract class BaseEntity {
    Long id;
    Instant createdAt;
    Instant updatedAt;
}
```

---

## 2. Enumerations

Define these as Java enums in `entity.enums`. In PostgreSQL they are stored as `VARCHAR` with a `CHECK` constraint (do not use native Postgres enum types — they are painful to alter).

| Enum            | Values                                                                     |
| --------------- | -------------------------------------------------------------------------- |
| `RoleName`      | `TEAM_MEMBER`, `MANAGER`, `ADMIN`                                          |
| `ProjectStatus` | `ACTIVE`, `INACTIVE`, `COMPLETED`                                          |
| `ReportStatus`  | `DRAFT`, `SUBMITTED`, `NEEDS_CORRECTION`, `APPROVED`                       |
| `TaskStatus`    | `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `BLOCKED`                       |
| `Priority`      | `LOW`, `MEDIUM`, `HIGH`                                                    |
| `ImpactLevel`   | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`                                        |
| `BlockerStatus` | `OPEN`, `RESOLVED`                                                         |
| `ReviewAction`  | `APPROVED`, `REQUEST_CHANGES`                                              |
| `WorkCategory`  | `DEVELOPMENT`, `TESTING`, `MEETINGS`, `DOCUMENTATION`, `RESEARCH`, `OTHER` |
| `InvitationStatus` | `PENDING`, `ACCEPTED`, `EXPIRED`, `REVOKED`                            |

---

## 3. Entity Reference

### 3.1 `roles`

Lookup table for system roles. Seeded once, never edited through the UI.

| Column        | Type         | Constraints      |
| ------------- | ------------ | ---------------- |
| `id`          | BIGINT       | PK, identity     |
| `name`        | VARCHAR(50)  | NOT NULL, UNIQUE |
| `description` | VARCHAR(255) | NULL             |
| `created_at`  | TIMESTAMP    | NOT NULL         |
| `updated_at`  | TIMESTAMP    | NOT NULL         |

**JPA:** `name` maps to `RoleName` enum, `@Enumerated(EnumType.STRING)`.

**Relationships:** `Role 1 ──< User`

---

### 3.2 `users`

| Column       | Type         | Constraints                |
| ------------ | ------------ | -------------------------- |
| `id`         | BIGINT       | PK, identity               |
| `name`       | VARCHAR(100) | NOT NULL                   |
| `email`      | VARCHAR(150) | NOT NULL, UNIQUE           |
| `password`   | VARCHAR(255) | NOT NULL, BCrypt hash      |
| `role_id`    | BIGINT       | NOT NULL, FK → `roles(id)` |
| `active`     | BOOLEAN      | NOT NULL, DEFAULT TRUE     |
| `failed_login_attempts` | INT | NOT NULL, DEFAULT 0 -- added `V22__add_login_lockout.sql` |
| `locked_until` | TIMESTAMP  | NULL -- added `V22__add_login_lockout.sql`                |
| `created_at` | TIMESTAMP    | NOT NULL                   |
| `updated_at` | TIMESTAMP    | NOT NULL                   |

**JPA notes:**

- `@ManyToOne(fetch = LAZY) @JoinColumn(name = "role_id")` → `Role role`
- `password` must be annotated `@JsonIgnore` and never appear in any DTO.
- The entity itself should **not** implement `UserDetails`. Create a separate `CustomUserDetails` wrapper in the security package.
- `failed_login_attempts` / `locked_until` back `LoginAttemptService` (per-account lockout, checked
  before every login attempt). Reset to `0` / `NULL` on a successful login.

**Indexes:** `idx_users_email` on `email`, `idx_users_role_id` on `role_id`.

**Relationships:**

- `Role 1 ──< User`
- `User 1 ──< UserProject`
- `User 1 ──< WeeklyReport` (as owner)
- `User 1 ──< ReportReview` (as reviewer)
- `User 1 ──< Invitation` (as `invited_by`)
- `User 1 ──< PasswordResetToken`
- `User 1 ──< RefreshToken`

---

### 3.3 `projects`

| Column        | Type         | Constraints                  |
| ------------- | ------------ | ---------------------------- |
| `id`          | BIGINT       | PK, identity                 |
| `name`        | VARCHAR(150) | NOT NULL, UNIQUE             |
| `description` | TEXT         | NULL                         |
| `status`      | VARCHAR(30)  | NOT NULL, DEFAULT `'ACTIVE'` |
| `created_at`  | TIMESTAMP    | NOT NULL                     |
| `updated_at`  | TIMESTAMP    | NOT NULL                     |

**Delete rule:** deleting a project referenced by any weekly report must be blocked. Implement as a soft delete — set `status = INACTIVE` instead of removing the row. The service layer must reject hard deletes when reports exist.

---

### 3.4 `user_projects`

Resolves the many-to-many between users and projects.

| Column        | Type      | Constraints                   |
| ------------- | --------- | ----------------------------- |
| `id`          | BIGINT    | PK, identity                  |
| `user_id`     | BIGINT    | NOT NULL, FK → `users(id)`    |
| `project_id`  | BIGINT    | NOT NULL, FK → `projects(id)` |
| `assigned_at` | TIMESTAMP | NOT NULL                      |
| `active`      | BOOLEAN   | NOT NULL, DEFAULT TRUE        |

**Constraint:** `UNIQUE (user_id, project_id)`

**JPA:** model this as a real entity `UserProject` with its own `id` — not a `@ManyToMany` with `@JoinTable`. It carries extra columns, so it needs to be a first-class entity.

---

### 3.5 `weekly_reports` — main table

| Column            | Type        | Constraints                         |
| ----------------- | ----------- | ----------------------------------- |
| `id`              | BIGINT      | PK, identity                        |
| `user_id`         | BIGINT      | NOT NULL, FK → `users(id)`          |
| `project_id`      | BIGINT      | NOT NULL, FK → `projects(id)`       |
| `week_start_date` | DATE        | NOT NULL                            |
| `week_end_date`   | DATE        | NOT NULL                            |
| `status`          | VARCHAR(30) | NOT NULL, DEFAULT `'DRAFT'`         |
| `summary`         | TEXT        | NULL                                |
| `notes`           | TEXT        | NULL — optional notes / links field |
| `current_version` | INTEGER     | NOT NULL, DEFAULT 1                 |
| `submitted_at`    | TIMESTAMP   | NULL                                |
| `created_at`      | TIMESTAMP   | NOT NULL                            |
| `updated_at`      | TIMESTAMP   | NOT NULL                            |

**Constraints:**

- `UNIQUE (user_id, project_id, week_start_date)` — one report per person, per project, per week.
- `CHECK (week_end_date >= week_start_date)`
- `week_start_date` must be a Monday — enforce in the service layer, not the DB.

**Indexes:** `idx_reports_user_id`, `idx_reports_project_id`, `idx_reports_status`, `idx_reports_week_start` (composite `idx_reports_week_status` on `(week_start_date, status)` for dashboard queries).

**JPA collections (all `mappedBy = "report"`, cascade ALL, orphanRemoval true):**

- `List<ReportTask> tasks`
- `List<NextWeekTask> nextWeekTasks`
- `List<Blocker> blockers`
- `List<Achievement> achievements`
- `List<WorkHour> workHours`
- `List<ReportReview> reviews`
- `List<ReportVersion> versions`

---

### 3.6 `report_tasks`

Tasks worked on during the reporting week.

| Column               | Type         | Constraints                                           |
| -------------------- | ------------ | ----------------------------------------------------- |
| `id`                 | BIGINT       | PK, identity                                          |
| `report_id`          | BIGINT       | NOT NULL, FK → `weekly_reports(id)` ON DELETE CASCADE |
| `task_name`          | VARCHAR(255) | NOT NULL                                              |
| `description`        | TEXT         | NULL                                                  |
| `status`             | VARCHAR(30)  | NOT NULL                                              |
| `priority`           | VARCHAR(30)  | NOT NULL                                              |
| `planned_percentage` | INTEGER      | NOT NULL, CHECK 0–100                                 |
| `actual_percentage`  | INTEGER      | NOT NULL, CHECK 0–100                                 |
| `hours_planned`      | DECIMAL(6,2) | NOT NULL, DEFAULT 0                                   |
| `hours_spent`        | DECIMAL(6,2) | NOT NULL, DEFAULT 0                                   |
| `deliverable`        | VARCHAR(500) | NULL — output produced                                |
| `created_at`         | TIMESTAMP    | NOT NULL                                              |
| `updated_at`         | TIMESTAMP    | NOT NULL                                              |

**Index:** `idx_report_tasks_report_id`

---

### 3.7 `next_week_tasks`

| Column        | Type         | Constraints                                           |
| ------------- | ------------ | ----------------------------------------------------- |
| `id`          | BIGINT       | PK, identity                                          |
| `report_id`   | BIGINT       | NOT NULL, FK → `weekly_reports(id)` ON DELETE CASCADE |
| `task_name`   | VARCHAR(255) | NOT NULL                                              |
| `description` | TEXT         | NULL                                                  |
| `priority`    | VARCHAR(30)  | NOT NULL, DEFAULT `'MEDIUM'`                          |
| `created_at`  | TIMESTAMP    | NOT NULL                                              |
| `updated_at`  | TIMESTAMP    | NOT NULL                                              |

**Index:** `idx_next_week_tasks_report_id`

---

### 3.8 `blockers`

| Column         | Type         | Constraints                                           |
| -------------- | ------------ | ----------------------------------------------------- |
| `id`           | BIGINT       | PK, identity                                          |
| `report_id`    | BIGINT       | NOT NULL, FK → `weekly_reports(id)` ON DELETE CASCADE |
| `title`        | VARCHAR(255) | NOT NULL                                              |
| `description`  | TEXT         | NULL                                                  |
| `impact`       | VARCHAR(30)  | NOT NULL                                              |
| `status`       | VARCHAR(30)  | NOT NULL, DEFAULT `'OPEN'`                            |
| `is_key_issue` | BOOLEAN      | NOT NULL, DEFAULT FALSE                               |
| `created_at`   | TIMESTAMP    | NOT NULL                                              |
| `updated_at`   | TIMESTAMP    | NOT NULL                                              |

**Business rule:** at most one blocker per report may have `is_key_issue = true`. Enforce in the service layer when saving a report; also add a partial unique index:
`CREATE UNIQUE INDEX uq_blocker_key ON blockers(report_id) WHERE is_key_issue = TRUE;`

**Index:** `idx_blockers_report_id`, `idx_blockers_status` (used for the "open blockers across team" dashboard metric).

---

### 3.9 `achievements`

| Column               | Type         | Constraints                                           |
| -------------------- | ------------ | ----------------------------------------------------- |
| `id`                 | BIGINT       | PK, identity                                          |
| `report_id`          | BIGINT       | NOT NULL, FK → `weekly_reports(id)` ON DELETE CASCADE |
| `title`              | VARCHAR(255) | NOT NULL                                              |
| `description`        | TEXT         | NULL                                                  |
| `is_key_achievement` | BOOLEAN      | NOT NULL, DEFAULT FALSE                               |
| `created_at`         | TIMESTAMP    | NOT NULL                                              |
| `updated_at`         | TIMESTAMP    | NOT NULL                                              |

**Business rule:** at most one achievement per report may have `is_key_achievement = true`. Same partial unique index pattern as blockers.

**Index:** `idx_achievements_report_id`

---

### 3.10 `work_hours`

| Column       | Type         | Constraints                                           |
| ------------ | ------------ | ----------------------------------------------------- |
| `id`         | BIGINT       | PK, identity                                          |
| `report_id`  | BIGINT       | NOT NULL, FK → `weekly_reports(id)` ON DELETE CASCADE |
| `task_type`  | VARCHAR(50)  | NOT NULL — `WorkCategory` enum                        |
| `hours`      | DECIMAL(6,2) | NOT NULL, CHECK `hours >= 0`                          |
| `created_at` | TIMESTAMP    | NOT NULL                                              |
| `updated_at` | TIMESTAMP    | NOT NULL                                              |

**Constraint:** `UNIQUE (report_id, task_type)` — one row per category per report.

**Index:** `idx_work_hours_report_id`

---

### 3.11 `report_reviews`

Full history of manager review actions. A report can be reviewed many times.

| Column           | Type        | Constraints                                            |
| ---------------- | ----------- | ------------------------------------------------------ |
| `id`             | BIGINT      | PK, identity                                           |
| `report_id`      | BIGINT      | NOT NULL, FK → `weekly_reports(id)` ON DELETE CASCADE  |
| `reviewer_id`    | BIGINT      | NOT NULL, FK → `users(id)`                             |
| `version_number` | INTEGER     | NOT NULL — which version this comment was made against |
| `action`         | VARCHAR(30) | NOT NULL — `ReviewAction`                              |
| `comment`        | TEXT        | NULL, required when action = `REQUEST_CHANGES`         |
| `reviewed_at`    | TIMESTAMP   | NOT NULL                                               |
| `created_at`     | TIMESTAMP   | NOT NULL                                               |
| `updated_at`     | TIMESTAMP   | NOT NULL                                               |

**`version_number` is required.** The assignment demands that a manager can see which version a given comment was made against. Copy `weekly_reports.current_version` into this column at review time.

**Validation:** `comment` must be non-blank when `action = REQUEST_CHANGES`. Enforce in the service layer.

**Indexes:** `idx_reviews_report_id`, `idx_reviews_reviewer_id`

**Relationships:** two separate `@ManyToOne` to `User` — one is the report owner (via report), one is the reviewer. Name the reviewer field `reviewer` and join on `reviewer_id`.

---

### 3.12 `report_versions`

**Required, not optional.** The assignment states past versions must remain visible alongside the version under review.

| Column           | Type      | Constraints                                           |
| ---------------- | --------- | ----------------------------------------------------- |
| `id`             | BIGINT    | PK, identity                                          |
| `report_id`      | BIGINT    | NOT NULL, FK → `weekly_reports(id)` ON DELETE CASCADE |
| `version_number` | INTEGER   | NOT NULL                                              |
| `snapshot_data`  | JSONB     | NOT NULL                                              |
| `submitted_at`   | TIMESTAMP | NOT NULL                                              |
| `created_at`     | TIMESTAMP | NOT NULL                                              |
| `updated_at`     | TIMESTAMP | NOT NULL                                              |

**Constraint:** `UNIQUE (report_id, version_number)`

**JPA mapping for JSONB:**

```
@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "jsonb")
private String snapshotData;
```

(Hibernate 6 supports this natively — no `hypersistence-utils` dependency needed.)

**Snapshot contents:** serialize the full report DTO — summary, notes, all tasks, next-week tasks, blockers, achievements, work hours — as JSON at the moment of submission.

**Index:** `idx_report_versions_report_id`

---

### 3.13 `invitations` (added `V19__create_invitations.sql`)

The only way an account can be created (besides the one-time bootstrap admin). Not a `BaseEntity` subclass in one respect only -- see JPA notes.

| Column        | Type         | Constraints                              |
| ------------- | ------------ | ----------------------------------------- |
| `id`          | BIGINT       | PK, identity                               |
| `email`       | VARCHAR(150) | NOT NULL                                   |
| `role_id`     | BIGINT       | NOT NULL, FK → `roles(id)`                 |
| `token_hash`  | VARCHAR(64)  | NOT NULL, UNIQUE -- SHA-256 hex of the raw token |
| `status`      | VARCHAR(30)  | NOT NULL, DEFAULT `'PENDING'` -- `InvitationStatus` |
| `expires_at`  | TIMESTAMP    | NOT NULL -- 48h from creation/resend       |
| `invited_by`  | BIGINT       | NOT NULL, FK → `users(id)`                 |
| `accepted_at` | TIMESTAMP    | NULL                                       |
| `created_at`  | TIMESTAMP    | NOT NULL                                   |
| `updated_at`  | TIMESTAMP    | NOT NULL                                   |

**JPA notes:** the raw token is never persisted -- only its SHA-256 hash (`SecureTokenService`), so
even full DB access doesn't hand out working invitation links. `resend` rotates `token_hash` and
`expires_at` in place rather than creating a new row, which invalidates the previous link
immediately. A pessimistic write lock (`findByTokenHashForUpdate`) guards `acceptInvitation` against
a concurrent double-accept race.

**Indexes:** unique on `token_hash`; index on `(email, status)` for the duplicate-pending-invite
check; index on `status` for the daily expiry sweep.

---

### 3.14 `password_reset_tokens` (added `V20__create_password_reset_tokens.sql`)

| Column       | Type        | Constraints                                       |
| ------------ | ----------- | -------------------------------------------------- |
| `id`         | BIGINT      | PK, identity                                        |
| `user_id`    | BIGINT      | NOT NULL, FK → `users(id)`                          |
| `token_hash` | VARCHAR(64) | NOT NULL, UNIQUE -- SHA-256 hex of the raw token     |
| `expires_at` | TIMESTAMP   | NOT NULL -- 30 min from creation                    |
| `used_at`    | TIMESTAMP   | NULL                                                 |
| `created_at` | TIMESTAMP   | NOT NULL                                             |

No `updated_at` -- a reset token is write-once (`used_at` is the only mutation, and it's set exactly
once). Requesting a new reset invalidates every prior unused token for that user
(`invalidateUnusedForUser`). A successful reset revokes every `refresh_tokens` row for that user in
the same transaction -- see 3.15.

**Index:** unique on `token_hash`.

---

### 3.15 `refresh_tokens` (added `V21__create_refresh_tokens.sql`)

Backs cookie-based auth + rotation. The `access_token` cookie (a short-lived JWT) is never persisted
anywhere; only the refresh token has server-side state, since only it needs to be revocable.

| Column          | Type         | Constraints                                          |
| --------------- | ------------ | ------------------------------------------------------ |
| `id`            | BIGINT       | PK, identity                                            |
| `user_id`       | BIGINT       | NOT NULL, FK → `users(id)`                              |
| `token_hash`    | VARCHAR(64)  | NOT NULL, UNIQUE -- SHA-256 hex of the raw token         |
| `family_id`     | UUID         | NOT NULL                                                |
| `expires_at`    | TIMESTAMP    | NOT NULL -- 7 days from issue                            |
| `revoked_at`    | TIMESTAMP    | NULL                                                     |
| `replaced_by_id`| BIGINT       | NULL, FK → `refresh_tokens(id)`                          |
| `created_at`    | TIMESTAMP    | NOT NULL                                                 |
| `user_agent`    | VARCHAR(255) | NULL                                                     |
| `ip_address`    | VARCHAR(45)  | NULL                                                     |

No `updated_at` -- like the reset token, this row is written once and mutated exactly once (marking
it revoked). `family_id` links every token descended from one original login: `rotate()` revokes the
presented token and issues the next one in the same family; presenting an already-revoked token is
treated as token theft and revokes the **entire family** in one bulk update (a separate
`REQUIRES_NEW`-propagation bean, since the revoke-then-throw sequence would otherwise get rolled back
by the enclosing transaction along with the exception it's raising). `revokeAllForUser` is called on
password reset, account deactivation, and role change -- any event that should kill every existing
session for that user.

**Indexes:** unique on `token_hash`; index on `user_id`; index on `family_id`.

---

## 4. Relationship Summary

```
Role         1 ──< User
User         1 ──< UserProject >── 1 Project
User         1 ──< WeeklyReport
Project      1 ──< WeeklyReport
WeeklyReport 1 ──< ReportTask
WeeklyReport 1 ──< NextWeekTask
WeeklyReport 1 ──< Blocker
WeeklyReport 1 ──< Achievement
WeeklyReport 1 ──< WorkHour
WeeklyReport 1 ──< ReportReview >── 1 User (reviewer)
WeeklyReport 1 ──< ReportVersion
User         1 ──< Invitation (invited_by)
User         1 ──< PasswordResetToken
User         1 ──< RefreshToken
```

---

## 5. Report Status Workflow

```
DRAFT ──submit──> SUBMITTED ──approve──────────> APPROVED
                      │
                      └──request changes──> NEEDS_CORRECTION
                                                   │
                                              edit + resubmit
                                                   │
                                                   └──> SUBMITTED
```

### Transition rules — implement in a single `ReportWorkflowService`

| From               | Action         | To                 | Side effects                                                 |
| ------------------ | -------------- | ------------------ | ------------------------------------------------------------ |
| —                  | create         | `DRAFT`            | `current_version = 1`                                        |
| `DRAFT`            | submit         | `SUBMITTED`        | write `ReportVersion` snapshot, set `submitted_at`           |
| `SUBMITTED`        | approve        | `APPROVED`         | write `ReportReview` with action `APPROVED`                  |
| `SUBMITTED`        | requestChanges | `NEEDS_CORRECTION` | write `ReportReview` with action `REQUEST_CHANGES` + comment |
| `NEEDS_CORRECTION` | edit           | `NEEDS_CORRECTION` | content mutable                                              |
| `NEEDS_CORRECTION` | resubmit       | `SUBMITTED`        | `current_version++`, write new `ReportVersion` snapshot      |

**Rules that must be enforced:**

- Any transition not in this table throws `InvalidStatusTransitionException` → HTTP 409.
- Report content (tasks, blockers, etc.) is editable **only** when status is `DRAFT` or `NEEDS_CORRECTION`. Any write attempt in `SUBMITTED` or `APPROVED` → HTTP 409.
- Controllers must never set `status` directly. All status changes route through `ReportWorkflowService.transition(...)`.

---

## 6. Access Control Rules (affects repository queries)

| Rule                                                                                | Enforcement                                                               |
| ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| A team member may read/write only reports where `user_id` = authenticated user id   | Ownership check in the service layer, not just the query                  |
| A manager/admin may read all reports                                                | Role check via `@PreAuthorize`                                            |
| A manager/admin may modify only `status` and review comments — never report content | Separate service methods; review endpoints must not accept content fields |
| Only admin may create/deactivate users and assign roles                             | `@PreAuthorize("hasRole('ADMIN')")`                                       |

Repository methods must include the owner in the query for member-scoped access, e.g. `findByIdAndUserId(Long id, Long userId)` — never `findById` followed by an in-memory check.

---

## 7. Flyway Migration Order

Create migrations in this order so foreign keys resolve:

```
V1__create_roles.sql
V2__create_users.sql
V3__create_projects.sql
V4__create_user_projects.sql
V5__create_weekly_reports.sql
V6__create_report_tasks.sql
V7__create_next_week_tasks.sql
V8__create_blockers.sql
V9__create_achievements.sql
V10__create_work_hours.sql
V11__create_report_reviews.sql
V12__create_report_versions.sql
V13__create_indexes.sql
V13_1__seed_roles.sql
V14__seed_data.sql              (db/seed, dev-profile only)
V15__seed_auth_data.sql         (db/seed, dev-profile only)
V16__seed_project_assignments.sql (db/seed, dev-profile only)
V17__add_review_acknowledged_at.sql
V18__dashboard_indexes.sql
V19__create_invitations.sql
V20__create_password_reset_tokens.sql
V21__create_refresh_tokens.sql
V22__add_login_lockout.sql
```

`db/seed` is only applied when the `dev` profile is active (see `application-dev.yml`'s
`spring.flyway.locations`) -- a production deployment runs with `db/migration` alone and gets no
demo accounts with known passwords.

Set `spring.jpa.hibernate.ddl-auto=validate`. Never `update` or `create-drop`.

---

## 8. Seed Data Requirements

`V14__seed_data.sql` must produce a dataset that makes the manager dashboard meaningful:

- 3 roles: `TEAM_MEMBER`, `MANAGER`, `ADMIN`
- 1 admin, 1 manager, 5 team members (all BCrypt-hashed passwords, documented in the README)
- 4 projects: Client A, Internal Tooling, R&D, Marketing
- User-project assignments so each member sits on 1–2 projects
- 6 consecutive weeks of reports across the 5 members, distributed across all four statuses, including:
  - at least 2 reports in `NEEDS_CORRECTION` with an existing manager comment
  - at least 1 report with 2+ versions and 2+ reviews, to demonstrate the correction cycle
  - at least 5 reports with `is_key_issue` blockers still `OPEN`
- Each report needs 3–6 tasks, 2–4 next-week tasks, 1–3 blockers, 1–3 achievements, and work-hour rows for at least 3 categories

---

## 9. Generation Checklist

When generating code from this file, produce in this order:

1. All enums in `entity.enums`
2. `BaseEntity` mapped superclass
3. All 12 entity classes with the exact columns above
4. One Spring Data JPA repository per entity, with the access-control-aware finder methods
5. Flyway migrations V1–V14 matching the entities exactly
6. Verify: start the app with `ddl-auto=validate` — it must boot with zero schema mismatch errors
