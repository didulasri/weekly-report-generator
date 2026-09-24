-- V18 was already taken by the dashboard-analytics module's V18__dashboard_indexes.sql (this
-- checkpoint's own instructions assumed V17 was the last one applied) -- continuing from V19.
CREATE TABLE invitations (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    invited_by BIGINT NOT NULL REFERENCES users(id),
    accepted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Only one PENDING invitation may exist per email at a time -- a repeat POST /api/admin/invitations
-- for the same address is rejected with a 409 telling the admin to use resend instead.
CREATE UNIQUE INDEX uq_invitations_pending_email ON invitations (lower(email)) WHERE status = 'PENDING';

CREATE INDEX idx_invitations_email ON invitations(lower(email));
CREATE INDEX idx_invitations_status ON invitations(status);
