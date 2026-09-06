-- users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);

-- weekly_reports
CREATE INDEX idx_reports_user_id ON weekly_reports(user_id);
CREATE INDEX idx_reports_project_id ON weekly_reports(project_id);
CREATE INDEX idx_reports_status ON weekly_reports(status);
CREATE INDEX idx_reports_week_start ON weekly_reports(week_start_date);
CREATE INDEX idx_reports_week_status ON weekly_reports(week_start_date, status);

-- report_tasks
CREATE INDEX idx_report_tasks_report_id ON report_tasks(report_id);

-- next_week_tasks
CREATE INDEX idx_next_week_tasks_report_id ON next_week_tasks(report_id);

-- blockers
CREATE INDEX idx_blockers_report_id ON blockers(report_id);
CREATE INDEX idx_blockers_status ON blockers(status);
CREATE UNIQUE INDEX uq_blocker_key ON blockers(report_id) WHERE is_key_issue = TRUE;

-- achievements
CREATE INDEX idx_achievements_report_id ON achievements(report_id);
CREATE UNIQUE INDEX uq_achievement_key ON achievements(report_id) WHERE is_key_achievement = TRUE;

-- work_hours
CREATE INDEX idx_work_hours_report_id ON work_hours(report_id);

-- report_reviews
CREATE INDEX idx_reviews_report_id ON report_reviews(report_id);
CREATE INDEX idx_reviews_reviewer_id ON report_reviews(reviewer_id);

-- report_versions
CREATE INDEX idx_report_versions_report_id ON report_versions(report_id);
