# Weekly Report Generator & Team Dashboard

# API Design Document

## 1. API Conventions

Base URL:

    /api

Content type:

    application/json

Authentication:

    Authorization: Bearer <JWT_TOKEN>

Recommended API versioning for future production systems:

    /api/v1

For this assignment, `/api` is sufficient.

---

# 2. Authentication APIs

## Register

    POST /api/auth/register

Purpose: Create a new user account.

Request:

    {
      "name": "John Doe",
      "email": "john@example.com",
      "password": "Password123"
    }

Response: 201 Created.

---

## Login

    POST /api/auth/login

Purpose: Authenticate user and return JWT.

Request:

    {
      "email": "john@example.com",
      "password": "Password123"
    }

Response:

    {
      "accessToken": "...",
      "tokenType": "Bearer",
      "user": {
        "id": 1,
        "name": "John Doe",
        "email": "john@example.com",
        "role": "TEAM_MEMBER"
      }
    }

---

## Current User

    GET /api/auth/me

Purpose: Get currently authenticated user.

---

## Refresh Token (Optional)

    POST /api/auth/refresh

---

## Logout (Optional)

    POST /api/auth/logout

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

## Create User

    POST /api/admin/users

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

1. `/api/auth/register`
2. `/api/auth/login`
3. `/api/auth/me`
4. `/api/projects`
5. `/api/reports`
6. `/api/reports/my`
7. `/api/reports/{id}`
8. `/api/reports/{id}/submit`
9. `/api/manager/reports`
10. `/api/manager/reports/{id}/approve`
11. `/api/manager/reports/{id}/request-changes`
12. Dashboard endpoints
13. Admin endpoints
14. Optional report version history endpoints

---

# 13. API Design Notes

- Use DTOs instead of returning JPA entities directly.
- Use pagination for list APIs.
- Validate ownership before allowing report access.
- Validate status transitions in the service layer.
- Keep URLs resource-oriented.
- Use query parameters for filtering.
- Keep authentication and authorization rules consistent.
