CREATE TABLE next_week_tasks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id BIGINT NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_next_week_tasks_report FOREIGN KEY (report_id) REFERENCES weekly_reports(id) ON DELETE CASCADE,
    CONSTRAINT chk_next_week_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);
