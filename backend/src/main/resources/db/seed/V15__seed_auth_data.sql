-- Auth demo users for the authentication/authorization step.
-- Password for all three: "Password123" (BCrypt-hashed below).
-- See backend/README.md for the credential list.
-- Roles (TEAM_MEMBER, MANAGER, ADMIN) already exist from V14__seed_data.sql.

INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
SELECT 'Admin User', 'admin@example.com', '$2b$10$h874idjuUPvyeli9C5i8w.grMv/jBQfrllK2HtNdqByZ/CliUNZu.', r.id, TRUE, now(), now()
FROM roles r WHERE r.name = 'ADMIN';

INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
SELECT 'Manager User', 'manager@example.com', '$2b$10$h874idjuUPvyeli9C5i8w.grMv/jBQfrllK2HtNdqByZ/CliUNZu.', r.id, TRUE, now(), now()
FROM roles r WHERE r.name = 'MANAGER';

INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
SELECT 'Member User', 'member@example.com', '$2b$10$h874idjuUPvyeli9C5i8w.grMv/jBQfrllK2HtNdqByZ/CliUNZu.', r.id, TRUE, now(), now()
FROM roles r WHERE r.name = 'TEAM_MEMBER';
