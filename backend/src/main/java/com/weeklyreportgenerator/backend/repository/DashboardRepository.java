package com.weeklyreportgenerator.backend.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;

// Not a per-entity repository -- every metric here spans multiple tables and is expressed as a
// single GROUP BY / aggregate query, never entities loaded into memory and reduced in Java. Plain
// Repository<WeeklyReport, Long> is used only as the required generic anchor; WeeklyReport carries
// no meaning for the methods below beyond satisfying Spring Data's interface contract.
public interface DashboardRepository extends Repository<WeeklyReport, Long> {

    // One native query for the whole compliance calculation: expected/onTime/late/pending are all
    // COUNT(*) FILTER aggregates over the same join, so this is a single round trip regardless of
    // team size. dueAt is computed once in Java (ComplianceDefinition.dueAt) and passed in, keeping
    // the on-time/late comparison itself in SQL.
    @Query(value = """
            SELECT
                COUNT(*) AS expected,
                COUNT(*) FILTER (WHERE r.id IS NOT NULL AND r.status <> 'DRAFT' AND r.submitted_at <= :dueAt) AS onTime,
                COUNT(*) FILTER (WHERE r.id IS NOT NULL AND r.status <> 'DRAFT' AND r.submitted_at > :dueAt) AS late,
                COUNT(*) FILTER (WHERE r.id IS NULL OR r.status = 'DRAFT') AS pending
            FROM user_projects up
            JOIN users u ON u.id = up.user_id AND u.active = true
            JOIN projects p ON p.id = up.project_id AND p.status = 'ACTIVE'
            LEFT JOIN weekly_reports r
                   ON r.user_id = up.user_id
                  AND r.project_id = up.project_id
                  AND r.week_start_date = :weekStart
            WHERE up.active = true
              AND (:projectId IS NULL OR up.project_id = :projectId)
            """, nativeQuery = true)
    ComplianceCounts getComplianceCounts(
            @Param("weekStart") LocalDate weekStart,
            @Param("dueAt") Instant dueAt,
            @Param("projectId") Long projectId);

    interface ComplianceCounts {
        Long getExpected();
        Long getOnTime();
        Long getLate();
        Long getPending();
    }

    // GROUP BY status for the week -- one query backs both the summary headline counts and the
    // status-summary endpoint's statusCounts block.
    @Query("SELECT r.status AS status, COUNT(r) AS cnt FROM WeeklyReport r "
            + "WHERE r.weekStartDate = :weekStart AND (:projectId IS NULL OR r.project.id = :projectId) "
            + "GROUP BY r.status")
    List<StatusCount> getStatusCounts(@Param("weekStart") LocalDate weekStart, @Param("projectId") Long projectId);

    interface StatusCount {
        ReportStatus getStatus();
        Long getCnt();
    }

    // Open/key-open blocker counts for the week, in one pass -- the second query the checkpoint
    // aims for alongside the status GROUP BY.
    @Query(value = """
            SELECT
                COUNT(*) FILTER (WHERE b.status = 'OPEN') AS openCount,
                COUNT(*) FILTER (WHERE b.status = 'OPEN' AND b.is_key_issue = true) AS keyCount
            FROM blockers b
            JOIN weekly_reports r ON r.id = b.report_id
            WHERE r.week_start_date = :weekStart
              AND (:projectId IS NULL OR r.project_id = :projectId)
            """, nativeQuery = true)
    BlockerCounts getBlockerCounts(@Param("weekStart") LocalDate weekStart, @Param("projectId") Long projectId);

    interface BlockerCounts {
        Long getOpenCount();
        Long getKeyCount();
    }

    // One row per active (user, project) assignment, each already reduced to per-status counts --
    // counts (not a single status) because a member assigned to more than one active project can
    // be submitted on one and notStarted on the other in the same week.
    @Query(value = """
            SELECT u.id AS userId, u.name AS name,
                COUNT(*) FILTER (WHERE r.status = 'DRAFT') AS draft,
                COUNT(*) FILTER (WHERE r.status = 'SUBMITTED') AS submitted,
                COUNT(*) FILTER (WHERE r.status = 'NEEDS_CORRECTION') AS needsCorrection,
                COUNT(*) FILTER (WHERE r.status = 'APPROVED') AS approved,
                COUNT(*) FILTER (WHERE r.id IS NULL) AS notStarted
            FROM users u
            JOIN user_projects up ON up.user_id = u.id AND up.active = true
            JOIN projects p ON p.id = up.project_id AND p.status = 'ACTIVE'
            LEFT JOIN weekly_reports r
                   ON r.user_id = up.user_id
                  AND r.project_id = up.project_id
                  AND r.week_start_date = :weekStart
            WHERE u.active = true
              AND (:projectId IS NULL OR up.project_id = :projectId)
            GROUP BY u.id, u.name
            ORDER BY u.name
            """, nativeQuery = true)
    List<MemberStatusCounts> getMemberStatusCounts(
            @Param("weekStart") LocalDate weekStart, @Param("projectId") Long projectId);

    interface MemberStatusCounts {
        Long getUserId();
        String getName();
        Long getDraft();
        Long getSubmitted();
        Long getNeedsCorrection();
        Long getApproved();
        Long getNotStarted();
    }

    // generate_series produces one row per week (Monday) across the whole range up front, then
    // LEFT JOINs report/task data onto it -- this is what gives "no gaps inside the range" without
    // any post-processing in Java. date_trunc('week', ...) in Postgres already buckets to Monday,
    // matching this app's week_start_date convention, so a caller passing a non-Monday date still
    // gets a correctly aligned series.
    @Query(value = """
            SELECT gs.week_start::date AS weekStartDate,
                COALESCE(SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completedTasks,
                COALESCE(COUNT(t.id), 0) AS totalTasks,
                COALESCE(SUM(t.hours_spent), 0) AS totalHoursSpent
            FROM generate_series(
                     date_trunc('week', CAST(:startDate AS date)),
                     date_trunc('week', CAST(:endDate AS date)),
                     interval '7 days') AS gs(week_start)
            LEFT JOIN weekly_reports r
                   ON r.week_start_date = gs.week_start::date
                  AND (:projectId IS NULL OR r.project_id = :projectId)
                  AND (:userId IS NULL OR r.user_id = :userId)
            LEFT JOIN report_tasks t ON t.report_id = r.id
            GROUP BY gs.week_start
            ORDER BY gs.week_start
            """, nativeQuery = true)
    List<TaskTrendRow> getTaskTrends(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);

    interface TaskTrendRow {
        LocalDate getWeekStartDate();
        Long getCompletedTasks();
        Long getTotalTasks();
        BigDecimal getTotalHoursSpent();
    }

    // LEFT JOIN from projects (not weekly_reports) so an active project with zero reports in range
    // still appears with zeroed metrics, instead of silently vanishing from the chart.
    @Query(value = """
            SELECT p.id AS projectId, p.name AS projectName,
                COUNT(DISTINCT r.id) AS reportCount,
                COUNT(t.id) AS taskCount,
                COALESCE(SUM(t.hours_spent), 0) AS totalHoursSpent,
                COUNT(DISTINCT r.user_id) AS memberCount
            FROM projects p
            LEFT JOIN weekly_reports r
                   ON r.project_id = p.id
                  AND r.week_start_date BETWEEN :startDate AND :endDate
            LEFT JOIN report_tasks t ON t.report_id = r.id
            WHERE p.status = 'ACTIVE'
              AND (:projectId IS NULL OR p.id = :projectId)
            GROUP BY p.id, p.name
            ORDER BY totalHoursSpent DESC
            """, nativeQuery = true)
    List<WorkloadRow> getWorkload(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("projectId") Long projectId);

    interface WorkloadRow {
        Long getProjectId();
        String getProjectName();
        Long getReportCount();
        Long getTaskCount();
        BigDecimal getTotalHoursSpent();
        Long getMemberCount();
    }

    // The window function SUM(...) OVER () computes the grand total alongside each group's own
    // sum in the same pass, so percentOfTotal is arithmetic done entirely in SQL, not Java.
    @Query(value = """
            SELECT w.task_type AS taskType,
                SUM(w.hours) AS totalHours,
                ROUND(SUM(w.hours) * 100.0 / NULLIF(SUM(SUM(w.hours)) OVER (), 0), 2) AS percentOfTotal
            FROM work_hours w
            JOIN weekly_reports r ON r.id = w.report_id
            WHERE r.week_start_date BETWEEN :startDate AND :endDate
              AND (:projectId IS NULL OR r.project_id = :projectId)
            GROUP BY w.task_type
            ORDER BY totalHours DESC
            """, nativeQuery = true)
    List<TimeDistributionRow> getTimeDistribution(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("projectId") Long projectId);

    interface TimeDistributionRow {
        String getTaskType();
        BigDecimal getTotalHours();
        BigDecimal getPercentOfTotal();
    }

    // One UNION ALL of the two activity sources, ordered and paginated entirely in SQL -- LIMIT/
    // OFFSET come from the Pageable, not a Java-side merge of two separately fetched lists. The
    // countQuery mirrors the same two row sets so total/totalPages stay accurate.
    //
    // ORDER BY occurred_at alone is unstable: rows sharing a timestamp (common with bulk-seeded or
    // same-second data) have no defined relative order, so Postgres can return them differently
    // between the page 0 and page 1 queries -- a row can then be skipped or duplicated across
    // pages. event_type then id (each source's own primary key) break every tie deterministically,
    // applied on the combined UNION ALL result (the outer ORDER BY on `feed`), not inside either
    // branch, since an ORDER BY on an individual branch would be discarded by the UNION anyway.
    @Query(value = """
            SELECT * FROM (
                SELECT 'SUBMISSION' AS event_type, rv.id AS id, r.id AS reportId, r.week_start_date AS weekStartDate,
                    owner.name AS memberName, p.name AS projectName, owner.name AS actorName,
                    'SUBMITTED' AS action, CAST(NULL AS varchar) AS comment, rv.submitted_at AS occurred_at
                FROM report_versions rv
                JOIN weekly_reports r ON r.id = rv.report_id
                JOIN users owner ON owner.id = r.user_id
                JOIN projects p ON p.id = r.project_id

                UNION ALL

                SELECT 'REVIEW' AS event_type, rr.id AS id, r.id AS reportId, r.week_start_date AS weekStartDate,
                    owner.name AS memberName, p.name AS projectName, reviewer.name AS actorName,
                    rr.action AS action, LEFT(rr.comment, 200) AS comment, rr.reviewed_at AS occurred_at
                FROM report_reviews rr
                JOIN weekly_reports r ON r.id = rr.report_id
                JOIN users owner ON owner.id = r.user_id
                JOIN users reviewer ON reviewer.id = rr.reviewer_id
                JOIN projects p ON p.id = r.project_id
            ) feed
            ORDER BY occurred_at DESC, event_type ASC, id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT rv.id FROM report_versions rv
                UNION ALL
                SELECT rr.id FROM report_reviews rr
            ) counted
            """,
            nativeQuery = true)
    Page<ActivityFeedRow> getActivityFeed(Pageable pageable);

    interface ActivityFeedRow {
        String getEventType();
        Long getReportId();
        LocalDate getWeekStartDate();
        String getMemberName();
        String getProjectName();
        String getActorName();
        String getAction();
        String getComment();
        Instant getOccurredAt();
    }

    // json_agg builds each member's ordered item list inside the same query that finds them --
    // one row per (user, project) assignment, items already a ready-to-parse JSON array (key-
    // flagged item first), '[]' when there's no report or the report has no blockers yet. The
    // service only has to parse the JSON text; it never assembles the list itself.
    @Query(value = """
            SELECT u.id AS userId, u.name AS name, p.name AS projectName,
                COALESCE(r.status, 'NOT_STARTED') AS reportStatus,
                COALESCE(
                    (SELECT json_agg(
                                json_build_object(
                                    'id', b.id, 'title', b.title, 'description', b.description,
                                    'impact', b.impact, 'status', b.status, 'keyFlag', b.is_key_issue
                                ) ORDER BY b.is_key_issue DESC, b.id
                            )
                     FROM blockers b WHERE b.report_id = r.id),
                    '[]'
                )::text AS items
            FROM users u
            JOIN user_projects up ON up.user_id = u.id AND up.active = true
            JOIN projects p ON p.id = up.project_id AND p.status = 'ACTIVE'
            LEFT JOIN weekly_reports r
                   ON r.user_id = u.id AND r.project_id = up.project_id AND r.week_start_date = :weekStart
            WHERE u.active = true
              AND (:projectId IS NULL OR up.project_id = :projectId)
            ORDER BY u.name
            """, nativeQuery = true)
    List<SectionComparisonRow> getBlockerComparison(
            @Param("weekStart") LocalDate weekStart, @Param("projectId") Long projectId);

    // Same shape as getBlockerComparison, sourced from achievements -- impact/status are always
    // null here since achievements have no equivalent columns.
    @Query(value = """
            SELECT u.id AS userId, u.name AS name, p.name AS projectName,
                COALESCE(r.status, 'NOT_STARTED') AS reportStatus,
                COALESCE(
                    (SELECT json_agg(
                                json_build_object(
                                    'id', a.id, 'title', a.title, 'description', a.description,
                                    'impact', CAST(NULL AS varchar), 'status', CAST(NULL AS varchar),
                                    'keyFlag', a.is_key_achievement
                                ) ORDER BY a.is_key_achievement DESC, a.id
                            )
                     FROM achievements a WHERE a.report_id = r.id),
                    '[]'
                )::text AS items
            FROM users u
            JOIN user_projects up ON up.user_id = u.id AND up.active = true
            JOIN projects p ON p.id = up.project_id AND p.status = 'ACTIVE'
            LEFT JOIN weekly_reports r
                   ON r.user_id = u.id AND r.project_id = up.project_id AND r.week_start_date = :weekStart
            WHERE u.active = true
              AND (:projectId IS NULL OR up.project_id = :projectId)
            ORDER BY u.name
            """, nativeQuery = true)
    List<SectionComparisonRow> getAchievementComparison(
            @Param("weekStart") LocalDate weekStart, @Param("projectId") Long projectId);

    interface SectionComparisonRow {
        Long getUserId();
        String getName();
        String getProjectName();
        String getReportStatus();
        String getItems();
    }
}
