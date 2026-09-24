-- Core reference data, needed in every environment (not demo data) -- split out from the old
-- V14__seed_data.sql so the three roles exist even when db/seed (dev-only demo accounts) is not
-- on the Flyway classpath. Versioned V13_1 so it sorts between V13 and V14, before the seed
-- migrations that look roles up by name.
INSERT INTO roles (name, description, created_at, updated_at) VALUES
    ('TEAM_MEMBER', 'Regular team member who submits weekly reports', now(), now()),
    ('MANAGER', 'Reviews and approves weekly reports', now(), now()),
    ('ADMIN', 'Manages users and roles', now(), now());
