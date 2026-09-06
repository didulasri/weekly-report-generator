CREATE TABLE report_reviews (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    action VARCHAR(30) NOT NULL,
    comment TEXT,
    reviewed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_report_reviews_report FOREIGN KEY (report_id) REFERENCES weekly_reports(id) ON DELETE CASCADE,
    CONSTRAINT fk_report_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT chk_report_reviews_action CHECK (action IN ('APPROVED', 'REQUEST_CHANGES'))
);
