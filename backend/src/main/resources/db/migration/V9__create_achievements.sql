CREATE TABLE achievements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    is_key_achievement BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_achievements_report FOREIGN KEY (report_id) REFERENCES weekly_reports(id) ON DELETE CASCADE
);
