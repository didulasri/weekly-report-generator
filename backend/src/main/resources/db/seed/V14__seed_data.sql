-- Seed data for local development and demo purposes ONLY -- loaded from db/seed, which is only
-- on the Flyway classpath in the dev profile (see application-dev.yml). Production never applies
-- this file, so it never contains these known passwords.
-- All seeded users share the password "Password123!" (BCrypt-hashed below).
-- See backend/README.md for the full credential list.
-- Roles are seeded separately by V13_1__seed_roles.sql, which always applies.

DO $$
DECLARE
    v_role_member_id BIGINT;
    v_role_manager_id BIGINT;
    v_role_admin_id BIGINT;
    v_admin_id BIGINT;
    v_manager_id BIGINT;
    v_member_ids BIGINT[] := ARRAY[]::BIGINT[];
    v_project_ids BIGINT[];
    v_password_hash TEXT := '$2b$10$nK2ikhrBxxhllr8.RjO6KuKiMZ6TpINWa50gn4AAXUrkV7IvT7RPu';
    v_week_starts DATE[] := ARRAY['2026-07-27','2026-08-03','2026-08-10','2026-08-17','2026-08-24','2026-08-31']::DATE[];
    v_statuses TEXT[] := ARRAY['DRAFT','SUBMITTED','NEEDS_CORRECTION','APPROVED'];
    v_user_id BIGINT;
    v_project_id BIGINT;
    v_report_id BIGINT;
    v_status TEXT;
    v_week_start DATE;
    v_week_end DATE;
    v_report_counter INT := 0;
    v_special_report_id BIGINT;
    i INT;
    j INT;
    k INT;
BEGIN
    SELECT id INTO v_role_member_id FROM roles WHERE name = 'TEAM_MEMBER';
    SELECT id INTO v_role_manager_id FROM roles WHERE name = 'MANAGER';
    SELECT id INTO v_role_admin_id FROM roles WHERE name = 'ADMIN';

    INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
    VALUES ('Alice Admin', 'admin@weeklyreport.com', v_password_hash, v_role_admin_id, TRUE, now(), now())
    RETURNING id INTO v_admin_id;

    INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
    VALUES ('Mark Manager', 'manager@weeklyreport.com', v_password_hash, v_role_manager_id, TRUE, now(), now())
    RETURNING id INTO v_manager_id;

    FOR i IN 1..5 LOOP
        INSERT INTO users (name, email, password, role_id, active, created_at, updated_at)
        VALUES ('Team Member ' || i, 'member' || i || '@weeklyreport.com', v_password_hash, v_role_member_id, TRUE, now(), now())
        RETURNING id INTO v_user_id;
        v_member_ids := array_append(v_member_ids, v_user_id);
    END LOOP;

    INSERT INTO projects (name, description, status, created_at, updated_at) VALUES
        ('Client A', 'External client engagement', 'ACTIVE', now(), now()),
        ('Internal Tooling', 'Internal developer tooling', 'ACTIVE', now(), now()),
        ('R&D', 'Research and development initiatives', 'ACTIVE', now(), now()),
        ('Marketing', 'Marketing site and campaigns', 'ACTIVE', now(), now());

    SELECT array_agg(id ORDER BY id) INTO v_project_ids FROM projects;
    -- v_project_ids[1]=Client A, [2]=Internal Tooling, [3]=R&D, [4]=Marketing

    INSERT INTO user_projects (user_id, project_id, assigned_at, active) VALUES
        (v_member_ids[1], v_project_ids[1], now(), TRUE),
        (v_member_ids[2], v_project_ids[2], now(), TRUE),
        (v_member_ids[2], v_project_ids[3], now(), TRUE),
        (v_member_ids[3], v_project_ids[1], now(), TRUE),
        (v_member_ids[3], v_project_ids[4], now(), TRUE),
        (v_member_ids[4], v_project_ids[3], now(), TRUE),
        (v_member_ids[5], v_project_ids[2], now(), TRUE),
        (v_member_ids[5], v_project_ids[4], now(), TRUE);

    FOR i IN 1..5 LOOP
        v_user_id := v_member_ids[i];
        v_project_id := CASE i
            WHEN 1 THEN v_project_ids[1]
            WHEN 2 THEN v_project_ids[2]
            WHEN 3 THEN v_project_ids[1]
            WHEN 4 THEN v_project_ids[3]
            WHEN 5 THEN v_project_ids[2]
        END;

        FOR j IN 1..6 LOOP
            v_report_counter := v_report_counter + 1;
            v_week_start := v_week_starts[j];
            v_week_end := v_week_start + 6;
            v_status := v_statuses[((i + j) % 4) + 1];

            INSERT INTO weekly_reports (
                user_id, project_id, week_start_date, week_end_date, status,
                summary, notes, current_version, submitted_at, created_at, updated_at
            ) VALUES (
                v_user_id, v_project_id, v_week_start, v_week_end, v_status,
                'Summary of work for week starting ' || v_week_start,
                'No additional notes.',
                1,
                CASE WHEN v_status <> 'DRAFT' THEN (v_week_end::timestamp + interval '9 hours') ELSE NULL END,
                now(), now()
            ) RETURNING id INTO v_report_id;

            FOR k IN 1..4 LOOP
                INSERT INTO report_tasks (
                    report_id, task_name, description, status, priority,
                    planned_percentage, actual_percentage, hours_planned, hours_spent,
                    deliverable, created_at, updated_at
                ) VALUES (
                    v_report_id,
                    'Task ' || k || ' - week ' || j,
                    'Description for task ' || k,
                    (ARRAY['NOT_STARTED','IN_PROGRESS','COMPLETED','BLOCKED'])[((k + j) % 4) + 1],
                    (ARRAY['LOW','MEDIUM','HIGH'])[((k + j) % 3) + 1],
                    LEAST(100, 20 * k),
                    LEAST(100, 15 * k),
                    8.00, 6.50,
                    'Deliverable output ' || k,
                    now(), now()
                );
            END LOOP;

            FOR k IN 1..3 LOOP
                INSERT INTO next_week_tasks (report_id, task_name, description, priority, created_at, updated_at)
                VALUES (
                    v_report_id,
                    'Planned task ' || k || ' for next week',
                    'Plan description ' || k,
                    (ARRAY['LOW','MEDIUM','HIGH'])[((k + j) % 3) + 1],
                    now(), now()
                );
            END LOOP;

            FOR k IN 1..2 LOOP
                IF k = 1 AND v_report_counter % 5 = 0 THEN
                    INSERT INTO blockers (report_id, title, description, impact, status, is_key_issue, created_at, updated_at)
                    VALUES (v_report_id, 'Key blocker for report ' || v_report_counter, 'Critical open issue blocking progress', 'HIGH', 'OPEN', TRUE, now(), now());
                ELSE
                    INSERT INTO blockers (report_id, title, description, impact, status, is_key_issue, created_at, updated_at)
                    VALUES (
                        v_report_id,
                        'Blocker ' || k || ' for report ' || v_report_counter,
                        'Description of blocker ' || k,
                        (ARRAY['LOW','MEDIUM','HIGH','CRITICAL'])[((k + j) % 4) + 1],
                        CASE WHEN k % 2 = 0 THEN 'RESOLVED' ELSE 'OPEN' END,
                        FALSE, now(), now()
                    );
                END IF;
            END LOOP;

            FOR k IN 1..2 LOOP
                INSERT INTO achievements (report_id, title, description, is_key_achievement, created_at, updated_at)
                VALUES (
                    v_report_id,
                    'Achievement ' || k || ' for report ' || v_report_counter,
                    'Description of achievement ' || k,
                    (k = 1),
                    now(), now()
                );
            END LOOP;

            INSERT INTO work_hours (report_id, task_type, hours, created_at, updated_at) VALUES
                (v_report_id, 'DEVELOPMENT', 24.00, now(), now()),
                (v_report_id, 'TESTING', 8.00, now(), now()),
                (v_report_id, 'MEETINGS', 6.00, now(), now());

            IF v_status = 'APPROVED' THEN
                INSERT INTO report_versions (report_id, version_number, snapshot_data, submitted_at, created_at, updated_at)
                VALUES (v_report_id, 1, '{"summary":"snapshot v1"}'::jsonb, (v_week_end::timestamp + interval '9 hours'), now(), now());

                INSERT INTO report_reviews (report_id, reviewer_id, version_number, action, comment, reviewed_at, created_at, updated_at)
                VALUES (v_report_id, v_manager_id, 1, 'APPROVED', NULL, (v_week_end::timestamp + interval '1 day'), now(), now());

            ELSIF v_status = 'NEEDS_CORRECTION' THEN
                INSERT INTO report_versions (report_id, version_number, snapshot_data, submitted_at, created_at, updated_at)
                VALUES (v_report_id, 1, '{"summary":"snapshot v1"}'::jsonb, (v_week_end::timestamp + interval '9 hours'), now(), now());

                INSERT INTO report_reviews (report_id, reviewer_id, version_number, action, comment, reviewed_at, created_at, updated_at)
                VALUES (v_report_id, v_manager_id, 1, 'REQUEST_CHANGES', 'Please add more detail on task outcomes and fix the hours breakdown.', (v_week_end::timestamp + interval '1 day'), now(), now());

                IF v_special_report_id IS NULL THEN
                    v_special_report_id := v_report_id;

                    UPDATE weekly_reports SET current_version = 2 WHERE id = v_report_id;

                    INSERT INTO report_versions (report_id, version_number, snapshot_data, submitted_at, created_at, updated_at)
                    VALUES (v_report_id, 2, '{"summary":"snapshot v2 - corrected"}'::jsonb, (v_week_end::timestamp + interval '3 days'), now(), now());

                    INSERT INTO report_reviews (report_id, reviewer_id, version_number, action, comment, reviewed_at, created_at, updated_at)
                    VALUES (v_report_id, v_manager_id, 2, 'REQUEST_CHANGES', 'Still missing the blocker impact assessment, please revise again.', (v_week_end::timestamp + interval '4 days'), now(), now());
                END IF;

            ELSIF v_status = 'SUBMITTED' THEN
                INSERT INTO report_versions (report_id, version_number, snapshot_data, submitted_at, created_at, updated_at)
                VALUES (v_report_id, 1, '{"summary":"snapshot v1"}'::jsonb, (v_week_end::timestamp + interval '9 hours'), now(), now());
            END IF;

        END LOOP;
    END LOOP;
END $$;
