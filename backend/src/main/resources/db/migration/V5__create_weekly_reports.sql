CREATE TABLE weekly_reports (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    summary TEXT,
    notes TEXT,
    current_version INTEGER NOT NULL DEFAULT 1,
    submitted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_weekly_reports_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_weekly_reports_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT uq_weekly_reports_user_project_week UNIQUE (user_id, project_id, week_start_date),
    CONSTRAINT chk_weekly_reports_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'NEEDS_CORRECTION', 'APPROVED')),
    CONSTRAINT chk_weekly_reports_week_range CHECK (week_end_date >= week_start_date)
);
