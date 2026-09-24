# Weekly Report Generator & Team Dashboard

# API Design Document

## 1. API Conventions

Base URL:

    /api

Content type:

    application/json

Authentication:

    httpOnly cookies: access_token (15 min) + refresh_token (7 days)

There is no `Authorization: Bearer` header support at all -- the browser
never has JavaScript-readable access to either token. `JwtAuthenticationFilter`
reads only the `access_token` cookie; an Authorization header, even a
well-formed one, is silently ignored (see Section 2, Cookie-Based
Authentication).

CSRF:

    X-XSRF-TOKEN: <value of the XSRF-TOKEN cookie>

Required on every state-changing request (POST/PUT/PATCH/DELETE) except
`/api/auth/login`, `/api/auth/forgot-password`, `/api/auth/reset-password`
and `/api/invitations/**` -- those are the pre-authentication endpoints a
visitor with no session yet must be able to call. A caller fetches the
`XSRF-TOKEN` cookie once via `GET /api/auth/csrf` and echoes its value back
in the header on every subsequent state-changing call (double-submit
cookie pattern; the cookie itself is deliberately not httpOnly so client
JS can read it, but it is not a credential -- it authorizes nothing by
itself, it only proves the request came from same-origin JS).

Recommended API versioning for future production systems:

    /api/v1

For this assignment, `/api` is sufficient.

---

# 2. Authentication APIs

## No public registration

There is no `POST /api/auth/register` and no other way for an anonymous
visitor to create an account. Accounts exist only through an admin
invitation (see the Invitations section) or the one-time bootstrap admin
created at startup from `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`.

Rationale: this is an internal team-reporting tool, not a public product --
open registration only creates attack surface (fake accounts, spam,
enumeration) with no corresponding benefit. Admin-issued invitations keep
account creation auditable (`invited_by`) and let the admin set the role
correctly from the start instead of trusting a self-reported one.

---

## Login

    POST /api/auth/login

Purpose: Authenticate user and set the session cookies. CSRF-exempt (a
visitor with no session can't have a CSRF token yet). Subject to the
per-IP login rate limit and to per-account lockout -- see Section 2a.

Request:

    {
      "email": "john@example.com",
      "password": "Password123"
    }

Response body -- no token anywhere in it, ever:

    {
      "id": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "role": "TEAM_MEMBER"
    }

Response headers set two httpOnly cookies:

    Set-Cookie: access_token=<jwt>; HttpOnly; Path=/api; SameSite=Lax; [Secure]
    Set-Cookie: refresh_token=<raw token>; HttpOnly; Path=/api/auth; SameSite=Strict; [Secure]

`Secure` is present whenever `app.cookie.secure=true` (always true outside
local HTTP dev). The refresh cookie is scoped to `/api/auth` only -- it is
never sent on ordinary API calls, only to `/api/auth/refresh` and
`/api/auth/logout`.

---

## Current User

    GET /api/auth/me

Purpose: Get currently authenticated user, read from the `access_token`
cookie.

---

## CSRF Token

    GET /api/auth/csrf

Purpose: Issue the `XSRF-TOKEN` cookie a caller needs before it can make
any state-changing request. Spring Security's CSRF token is deferred (not
generated or written to a cookie until something reads it), so a client
must call this once -- typically right after login, or on app load -- before
attempting a POST/PUT/PATCH/DELETE outside the CSRF-exempt list.

---

## Refresh Token

    POST /api/auth/refresh

Purpose: Rotate the refresh token and issue a new access token, without
requiring the password again. Reads the `refresh_token` cookie; requires
the `X-XSRF-TOKEN` header (this endpoint is CSRF-protected even though it's
in Spring Security's `permitAll()` list -- "doesn't need a valid access
token" and "doesn't need CSRF protection" are independent concerns).

Response: same two `Set-Cookie` headers as login, with fresh values. The
previous refresh token is revoked as part of the same rotation.

Reuse detection: presenting a refresh token that was already rotated (i.e.
already revoked) is treated as evidence the token was stolen and replayed
-- the entire token family (every token descended from the same original
login) is revoked immediately, forcing a fresh login on every device that
was using that session.

Failure: 401, with both cookies cleared in the response (`Set-Cookie` with
`Max-Age=0`) -- whatever the browser was holding is no longer usable.

---

## Logout

    POST /api/auth/logout

Purpose: Revoke the current refresh token and clear both cookies. Requires
`X-XSRF-TOKEN` (same reasoning as refresh).

---

# 2a. Rate Limiting & Account Lockout

Two independent layers, both introduced alongside the invitation/reset/
cookie work:

## Per-IP rate limiting

In-memory (single-instance) token buckets, one per client IP per endpoint,
applied only to the two unauthenticated endpoints that can be hammered:

| Endpoint                    | Default limit  |
| ---------------------------- | -------------- |
| `POST /api/auth/login`       | 10 / minute    |
| `POST /api/auth/forgot-password` | 5 / minute |

Exceeding the limit returns `429 Too Many Requests` with a `Retry-After: 60`
header. Configurable via `LOGIN_RATE_LIMIT_PER_MINUTE` /
`FORGOT_PASSWORD_RATE_LIMIT_PER_MINUTE`.

## Per-account lockout

Tracked on the `users` row (`failed_login_attempts`, `locked_until`), so it
survives an app restart. After 5 consecutive failed login attempts for one
account, that account is locked for 15 minutes -- checked *before* every
authentication attempt, so a correct password submitted mid-lockout is
still rejected. A successful login resets the counter. Configurable via
`LOGIN_LOCKOUT_MAX_ATTEMPTS` / `LOGIN_LOCKOUT_DURATION_MINUTES`.

Locked-out response: `423 Locked`.

    {
      "status": 423,
      "error": "LOCKED",
      "message": "Account temporarily locked due to too many failed login attempts. Try again in 14 minute(s)."
    }

---

# 2b. Invitations

No account can be created except through this flow (plus the one-time
bootstrap admin). All invitation endpoints are POST + JSON body, never
GET + query string, so the token never lands in a server access log.

## Create Invitation (admin)

    POST /api/admin/invitations

Access: ADMIN only.

Request:

    {
      "email": "newhire@example.com",
      "role": "TEAM_MEMBER"
    }

409 if an active account with that email already exists, or a PENDING
invitation for it already exists (use resend instead). Sends an email
containing a link to `<frontend-url>/accept-invitation?token=...`; the
token is single-use, expires after 48 hours, and only its SHA-256 hash is
ever stored.

## List Invitations (admin)

    GET /api/admin/invitations?status=PENDING&page=0&size=10

Access: ADMIN only. Stale PENDING invitations are lazily marked EXPIRED on
every list call (and once daily via a scheduled job) before the query runs.

## Resend Invitation (admin)

    POST /api/admin/invitations/{id}/resend

Access: ADMIN only. Rotates the token (the old link stops working
immediately) and re-sends the email with a fresh 48-hour expiry. Allowed
from PENDING or EXPIRED status.

## Revoke Invitation (admin)

    DELETE /api/admin/invitations/{id}

Access: ADMIN only. Sets status to REVOKED; the token becomes unusable.

## Validate Invitation (anonymous)

    POST /api/invitations/validate

Request: `{ "token": "..." }`. Returns the invited email/role/expiry if
still usable, else `410 Gone`. Used by the accept-invitation page to show
who's being invited before asking for a name/password.

## Accept Invitation (anonymous)

    POST /api/invitations/accept

Request:

    {
      "token": "...",
      "name": "New Hire",
      "password": "Password123"
    }

Creates the account with the role fixed by the invitation (a `role` field
in the request body, if sent, is ignored -- it's not part of the DTO).
Returns `201` with the new user summary. `410 Gone` for an unknown,
expired, revoked, or already-accepted token.

---

# 2c. Password Reset

## Request Reset (self-service)

    POST /api/auth/forgot-password

CSRF-exempt. Request: `{ "email": "..." }`. Always returns `202` with the
same body regardless of whether the email belongs to an active account --
this endpoint must never be usable to discover which emails have accounts.
Subject to the per-IP rate limit (Section 2a). A reset link is emailed
only for a known, active account; the token expires after 30 minutes and
only its hash is stored. Requesting a second reset invalidates the first
token.

## Reset Password

    POST /api/auth/reset-password

CSRF-exempt (a visitor mid-reset has no session yet). Request:

    {
      "token": "...",
      "newPassword": "NewPassword2"
    }

`204` on success. `410 Gone` for an unknown, expired, or already-used
token. On success, every refresh token for that user is revoked -- a
password reset logs out every other device/session immediately, not just
the one performing the reset.

## Trigger Reset (admin)

    POST /api/admin/users/{id}/send-password-reset

Access: ADMIN only. Triggers the exact same email flow as the self-service
endpoint above -- the admin never sets or sees the new password.

---

# 3. Project APIs

## Get Projects

    GET /api/projects

Access:

- TEAM_MEMBER
- MANAGER
- ADMIN

Optional filters:

    GET /api/projects?active=true

---

## Create Project

    POST /api/projects

Access:

- MANAGER
- ADMIN

Request:

    {
      "name": "Internal Platform",
      "description": "Internal engineering project"
    }

---

## Get Project

    GET /api/projects/{id}

---

## Update Project

    PUT /api/projects/{id}

Access:

- MANAGER
- ADMIN

---

## Delete/Deactivate Project

    DELETE /api/projects/{id}

Access:

- MANAGER
- ADMIN

Recommended: Prefer soft delete/deactivation if projects have historical reports.

---

# 4. Weekly Report APIs

## Create Draft

    POST /api/reports

Access:

- TEAM_MEMBER

Request includes:

- week start/end.
- project.
- notes.
- report sub-items.

Initial status:

    DRAFT

---

## Get My Reports

    GET /api/reports/my

Optional query parameters:

    ?page=0
    &size=10
    &status=APPROVED
    &projectId=1
    &startDate=2026-09-01
    &endDate=2026-09-30

---

## Get Single Report

    GET /api/reports/{id}

Authorization:

- Owner.
- Authorized manager.
- Admin.

---

## Update Report

    PUT /api/reports/{id}

Allowed:

- DRAFT.
- NEEDS_CORRECTION.

---

## Submit Report

    POST /api/reports/{id}/submit

Status transition:

    DRAFT -> SUBMITTED
    NEEDS_CORRECTION -> SUBMITTED

---

## Delete Draft

    DELETE /api/reports/{id}

Recommended rule:

- Only owner.
- Only DRAFT status.

---

## Get Report History

    GET /api/reports/{id}/history

Returns:

- Review history.
- Optional version history.

---

# 5. Report Sub-Resources — Design Decision

Tasks, next-week tasks, blockers, achievements and work hours are NOT
exposed as separate endpoints. They are children of the report aggregate
and are created, replaced and deleted through:

    POST /api/reports
    PUT  /api/reports/{id}

Rationale: the report form is edited and saved as a single unit, so one
transactional write is both correct and simpler. This keeps version
snapshots atomic, makes the single-key-blocker rule enforceable without
a race condition, and puts the ownership and editable-status guard in
one place instead of twenty.

Per-row endpoints can be added later without changing this contract if
the UI ever needs inline autosave.

# 6. Manager Report APIs

All manager endpoints require MANAGER or ADMIN.

## Get Team Reports

    GET /api/manager/reports

Filters:

    ?page=0
    &size=10
    &status=SUBMITTED
    &projectId=1
    &userId=5
    &startDate=2026-09-01
    &endDate=2026-09-30

---

## Get Report for Review

    GET /api/manager/reports/{id}

---

## Approve Report

    POST /api/manager/reports/{id}/approve

Request:

    {
      "comment": "Good work. Approved."
    }

Transition:

    SUBMITTED -> APPROVED

---

## Request Changes

    POST /api/manager/reports/{id}/request-changes

Request:

    {
      "comment": "Please add more details about testing."
    }

Transition:

    SUBMITTED -> NEEDS_CORRECTION

Validation:

- Comment should be required.

---

## Get Team Members

    GET /api/manager/team-members

Optional filters:

    ?page=0&size=10

---

# 7. Dashboard APIs

## Dashboard Summary

    GET /api/dashboard/summary

Returns:

- Total reports.
- Submitted reports.
- Approved reports.
- Reports needing correction.
- Open blockers.
- Compliance rate.

---

## Task Trends

    GET /api/dashboard/task-trends

Optional:

    ?startDate=...
    &endDate=...
    &projectId=...

---

## Status Summary

    GET /api/dashboard/status-summary

Returns report counts grouped by status.

---

## Workload by Project

    GET /api/dashboard/workload

---

## Time Distribution

    GET /api/dashboard/time-distribution

Returns hours grouped by task type.

---

# 8. User Management APIs

Admin endpoints.

## Get Users

    GET /api/admin/users

## Get User

    GET /api/admin/users/{id}

No `POST /api/admin/users`: account creation is invitation-only (see the
Invitations section). An admin never sets or sees a user's password.

## Update User

    PUT /api/admin/users/{id}

## Update User Role

    PATCH /api/admin/users/{id}/role

Request:

    {
      "role": "MANAGER"
    }

## Delete/Deactivate User

    DELETE /api/admin/users/{id}

Soft delete: sets `active = false`. The row, and every report/review/version
that references this user, is never removed.

---

## Reactivate User

    PATCH /api/admin/users/{id}/activate

Sets `active = true`. Deactivation with no way back would be a dead end.

---

## Trigger Password Reset

    POST /api/admin/users/{id}/send-password-reset

Admin-initiated reset for a locked-out user. Sends the same reset-password
email as the self-service `/api/auth/forgot-password` flow -- the admin
never sets or sees the new password themselves. See the Password Reset
section.

---

# 9. Standard HTTP Responses

## Success

- 200 OK: successful GET/PUT/PATCH.
- 201 Created: successful POST creation.
- 204 No Content: successful deletion.

## Client errors

- 400 Bad Request.
- 401 Unauthorized.
- 403 Forbidden.
- 404 Not Found.
- 409 Conflict.
- 410 Gone -- an invitation or password-reset token that is unknown,
  expired, revoked, or already used.
- 423 Locked -- an account past its failed-login-attempt threshold.
- 429 Too Many Requests -- the per-IP rate limit on login or
  forgot-password.

## Server error

- 500 Internal Server Error.

---

# 10. Standard Error Response

Recommended format:

    {
      "timestamp": "2026-09-06T12:00:00",
      "status": 400,
      "error": "VALIDATION_ERROR",
      "message": "Week start date is required",
      "path": "/api/reports"
    }

---

# 11. API Security Matrix

| Endpoint Group       | TEAM_MEMBER | MANAGER    | ADMIN |
| -------------------- | ----------- | ---------- | ----- |
| Authentication       | Yes         | Yes        | Yes   |
| Own Reports          | Yes         | Yes        | Yes   |
| Other Users' Reports | No          | Yes        | Yes   |
| Manager Review       | No          | Yes        | Yes   |
| Dashboard            | No          | Yes        | Yes   |
| Project Management   | Read        | Full       | Full  |
| User Management      | No          | Limited/No | Full  |

---

# 12. Recommended Endpoint Development Order

1. `/api/auth/login`
2. `/api/auth/me`
3. `/api/projects`
4. `/api/reports`
5. `/api/reports/my`
6. `/api/reports/{id}`
7. `/api/reports/{id}/submit`
8. `/api/manager/reports`
9. `/api/manager/reports/{id}/approve`
10. `/api/manager/reports/{id}/request-changes`
11. Dashboard endpoints
12. Admin endpoints
13. Optional report version history endpoints
14. Invitations (admin + public accept flow)
15. Password reset (self-service + admin-triggered)
16. Cookie-based auth + refresh rotation + CSRF
17. Rate limiting + account lockout

---

# 13. API Design Notes

- Use DTOs instead of returning JPA entities directly.
- Use pagination for list APIs.
- Validate ownership before allowing report access.
- Validate status transitions in the service layer.
- Keep URLs resource-oriented.
- Use query parameters for filtering.
- Keep authentication and authorization rules consistent.
