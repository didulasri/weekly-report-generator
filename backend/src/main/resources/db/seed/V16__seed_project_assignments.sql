-- Project assignments for the auth-step seeded team member (member@example.com,
-- from V15__seed_auth_data.sql). The 4 projects themselves and the 5-member
-- demo dataset already exist from V14__seed_data.sql, so this migration only
-- adds assignments -- it does not re-insert projects.https://claude.ai/new

INSERT INTO user_projects (user_id, project_id, assigned_at, active)
SELECT u.id, p.id, now(), TRUE
FROM users u, projects p
WHERE u.email = 'member@example.com' AND p.name = 'Client A';

INSERT INTO user_projects (user_id, project_id, assigned_at, active)
SELECT u.id, p.id, now(), TRUE
FROM users u, projects p
WHERE u.email = 'member@example.com' AND p.name = 'Internal Tooling';
