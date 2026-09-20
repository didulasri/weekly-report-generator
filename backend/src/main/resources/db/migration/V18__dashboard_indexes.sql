-- blockers(status) already exists (idx_blockers_status, from V13) -- not repeated here.

-- Backs the activity-feed's ORDER BY occurred_at (each source's own timestamp column) and any
-- future range filter on submission/review time.
CREATE INDEX idx_report_versions_submitted_at ON report_versions(submitted_at);
CREATE INDEX idx_report_reviews_reviewed_at ON report_reviews(reviewed_at);
