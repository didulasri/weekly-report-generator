# Weekly Report Generator & Team Dashboard
# System Design Document

## 1. Project Overview

The Weekly Report Generator & Team Dashboard is a full-stack web application that helps team members create structured weekly reports and allows managers to review team progress.

### Primary goals
- Standardize weekly reporting.
- Allow users to save reports as drafts and submit them.
- Allow managers to approve reports or request corrections.
- Track tasks, blockers, achievements, work hours, and next-week plans.
- Provide a dashboard for team-level analysis.

---

## 2. System Scope

### In scope
- Authentication and role-based authorization.
- User management.
- Project management.
- Weekly report creation and editing.
- Draft and submission workflow.
- Manager review workflow.
- Correction and resubmission workflow.
- Report history.
- Dashboard analytics.
- Filtering and pagination.
- Seed data.

### Optional / bonus scope
- Report version history.
- Admin user management.
- Docker deployment.
- AI assistant.

---

## 3. User Roles

### TEAM_MEMBER
Can:
- Register and log in.
- Create weekly reports.
- Save reports as drafts.
- Edit own draft or correction-requested reports.
- Submit and resubmit reports.
- View own report history.
- View manager comments.

Cannot:
- Review or approve reports.
- Access manager dashboard.
- Access other users' reports.

### MANAGER
Can:
- View team reports.
- Filter reports.
- Review submitted reports.
- Approve reports.
- Request corrections with comments.
- View dashboard analytics.
- Manage projects.

### ADMIN
Recommended permissions:
- All manager permissions.
- Manage users and roles.
- Manage projects.
- View all reports.

---

## 4. System Architecture

The system uses a three-tier architecture:

    React Frontend
          |
          | HTTPS / REST JSON
          v
    Spring Boot Backend
          |
          | JPA / Hibernate
          v
      PostgreSQL

### Frontend responsibilities
- User interface.
- Routing.
- Form validation.
- API communication.
- Authentication state.
- Role-based UI.
- Dashboard charts.

### Backend responsibilities
- REST APIs.
- Business logic.
- Authentication.
- Authorization.
- Validation.
- Report workflow enforcement.
- Database access.
- Exception handling.

### Database responsibilities
- Persistent storage.
- Entity relationships.
- Constraints.
- Data integrity.

---

## 5. Main Functional Modules

### 5.1 Authentication & Authorization
Features:
- Registration.
- Login.
- Password hashing.
- JWT authentication.
- Role-based access control.

### 5.2 Project Management
Features:
- Create project.
- View projects.
- Update project.
- Delete/deactivate project.

### 5.3 Weekly Report Management
A report contains:
- Reporting week.
- Project.
- Tasks completed/current tasks.
- Planned vs actual progress.
- Next-week tasks.
- Blockers.
- Achievements.
- Work hours.
- Notes.

### 5.4 Review Workflow
Managers can:
- View submitted reports.
- Approve reports.
- Request changes.
- Add review comments.

### 5.5 Dashboard
Managers can view:
- Total submitted reports.
- Reports by status.
- Compliance rate.
- Open blockers.
- Task completion trends.
- Workload by project.
- Time distribution.

### 5.6 Report Version History (Optional)
Stores historical snapshots when reports are resubmitted.

---

## 6. Report Lifecycle

    DRAFT
      |
      | Submit
      v
    SUBMITTED
      |             |
      | Approve     | Request Changes
      v             v
    APPROVED   NEEDS_CORRECTION
                      |
                      | Edit + Resubmit
                      v
                  SUBMITTED

### Status rules
- DRAFT: Owner can edit.
- SUBMITTED: Waiting for manager review.
- APPROVED: Locked from normal editing.
- NEEDS_CORRECTION: Owner can edit and resubmit.

---

## 7. Core Business Rules

1. A team member can only access their own reports.
2. Managers can review reports assigned to their accessible team scope.
3. Only SUBMITTED reports can be approved or sent back for corrections.
4. A correction request should contain a manager comment.
5. Approved reports cannot be edited by normal users.
6. A user cannot create duplicate reports for the same reporting week and project unless the business requirement explicitly allows it.
7. Percentages should be validated between 0 and 100.
8. Work hours cannot be negative.
9. Email addresses must be unique.
10. Passwords must never be stored as plain text.

---

## 8. Database Design

### Main entities
- ROLE
- USER
- PROJECT
- WEEKLY_REPORT
- REPORT_TASK
- NEXT_WEEK_TASK
- BLOCKER
- ACHIEVEMENT
- WORK_HOUR
- REPORT_REVIEW
- REPORT_VERSION (optional)

### Relationship summary

    ROLE 1 -------- * USER
    USER 1 -------- * WEEKLY_REPORT
    PROJECT 1 ----- * WEEKLY_REPORT
    WEEKLY_REPORT 1-* REPORT_TASK
    WEEKLY_REPORT 1-* NEXT_WEEK_TASK
    WEEKLY_REPORT 1-* BLOCKER
    WEEKLY_REPORT 1-* ACHIEVEMENT
    WEEKLY_REPORT 1-* WORK_HOUR
    WEEKLY_REPORT 1-* REPORT_REVIEW
    USER 1 -------- * REPORT_REVIEW
    WEEKLY_REPORT 1-* REPORT_VERSION

---

## 9. Data Ownership

### User
Owns:
- Weekly reports.

### Weekly Report
Owns:
- Tasks.
- Next-week tasks.
- Blockers.
- Achievements.
- Work hours.
- Reviews.
- Versions.

---

## 10. Recommended Backend Architecture

    Controller
        |
        v
      Service
        |
        v
    Repository
        |
        v
     PostgreSQL

### Controller
- Accepts HTTP requests.
- Validates request input.
- Returns HTTP responses.
- Does not contain heavy business logic.

### Service
- Contains business rules.
- Controls report status transitions.
- Performs authorization checks where appropriate.

### Repository
- Database access through Spring Data JPA.

### DTOs
Use request/response DTOs instead of exposing entities directly.

---

## 11. Recommended Frontend Architecture

    pages/
    components/
    services/
    hooks/
    context/
    routes/

### Pages
Route-level screens.

### Components
Reusable UI components.

### Services
API calls using Axios.

### Context
Authentication and shared application state.

### Routes
Protected and role-based routing.

---

## 12. Security Design

### Authentication
- JWT-based authentication.
- Password hashing using BCrypt.

### Authorization
Protect endpoints by role:
- TEAM_MEMBER
- MANAGER
- ADMIN

### Additional security
- Validate all request data.
- Do not expose password fields.
- Use DTOs.
- Return proper HTTP status codes.
- Configure CORS for the React application.
- Store secrets in environment variables in production.

---

## 13. Error Handling

Use centralized exception handling.

Examples:
- 400 Bad Request: validation errors.
- 401 Unauthorized: invalid/missing authentication.
- 403 Forbidden: insufficient permission.
- 404 Not Found: resource does not exist.
- 409 Conflict: duplicate/conflicting resource.
- 500 Internal Server Error: unexpected error.

Recommended standard error response:

    {
      "timestamp": "...",
      "status": 400,
      "message": "...",
      "path": "/api/..."
    }

---

## 14. Non-Functional Requirements

### Performance
- Use pagination for large report lists.
- Use efficient database queries.
- Add indexes to commonly filtered columns.

### Maintainability
- Use layered architecture.
- Use DTOs.
- Keep modules separated.
- Use meaningful naming.

### Scalability
The monolith can later be expanded with:
- Separate frontend deployment.
- Redis caching.
- Background jobs.
- Object storage.
- Microservices if justified.

---

## 15. Deployment Architecture

Development:

    React (localhost)
         |
         v
    Spring Boot (localhost)
         |
         v
    PostgreSQL (local)

Production recommendation:

    React Build
         |
         v
    Web Server / CDN
         |
         v
    Spring Boot API
         |
         v
    Managed PostgreSQL

Docker Compose is optional and useful for consistent local setup.

---

## 16. Recommended Development Order

1. Finalize requirements.
2. Finalize ER diagram.
3. Create database schema/migrations.
4. Create Spring Boot project.
5. Configure PostgreSQL.
6. Implement roles and users.
7. Implement authentication.
8. Implement authorization.
9. Implement projects.
10. Implement report draft CRUD.
11. Implement report submission.
12. Implement manager review.
13. Implement correction/resubmission.
14. Test the complete backend workflow.
15. Build React authentication.
16. Build team member pages.
17. Build manager pages.
18. Connect frontend to backend.
19. Build dashboard analytics.
20. Add filtering and pagination.
21. Add seed data.
22. Test and deploy.

---

## 17. Definition of MVP

The MVP is complete when this full flow works:

    Team Member
        -> Create Report
        -> Save Draft
        -> Submit
    Manager
        -> Review
        -> Request Changes OR Approve
    Team Member
        -> Edit
        -> Resubmit
    Manager
        -> Approve

After this workflow works, build the dashboard and optional features.
