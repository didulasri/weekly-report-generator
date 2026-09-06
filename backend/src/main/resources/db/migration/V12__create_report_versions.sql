CREATE TABLE report_versions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    snapshot_data JSONB NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_report_versions_report FOREIGN KEY (report_id) REFERENCES weekly_reports(id) ON DELETE CASCADE,
    CONSTRAINT uq_report_versions_report_version UNIQUE (report_id, version_number)
);
