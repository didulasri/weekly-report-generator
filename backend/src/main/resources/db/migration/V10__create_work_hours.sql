CREATE TABLE work_hours (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id BIGINT NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    hours DECIMAL(6,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_work_hours_report FOREIGN KEY (report_id) REFERENCES weekly_reports(id) ON DELETE CASCADE,
    CONSTRAINT uq_work_hours_report_task_type UNIQUE (report_id, task_type),
    CONSTRAINT chk_work_hours_task_type CHECK (task_type IN ('DEVELOPMENT', 'TESTING', 'MEETINGS', 'DOCUMENTATION', 'RESEARCH', 'OTHER')),
    CONSTRAINT chk_work_hours_hours CHECK (hours >= 0)
);
