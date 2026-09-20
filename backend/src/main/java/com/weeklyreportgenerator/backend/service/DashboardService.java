package com.weeklyreportgenerator.backend.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.dto.dashboard.ActivityFeedEntryResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.ComplianceResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.DashboardSummaryResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.MemberStatusBreakdownResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.SectionComparisonResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.SectionItemResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.SectionType;
import com.weeklyreportgenerator.backend.dto.dashboard.StatusSummaryResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.TaskTrendResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.TimeDistributionResponse;
import com.weeklyreportgenerator.backend.dto.dashboard.WorkloadResponse;
import com.weeklyreportgenerator.backend.dto.response.PagedResponse;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;
import com.weeklyreportgenerator.backend.repository.DashboardRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

// summary and status-summary are deliberately week-scoped only (weekStart + projectId), not a
// date range: notStartedCount and the per-member breakdown only have an unambiguous meaning for a
// single week (expected members vs. one report row per member per week). Range-based metrics
// (trends, workload, time-distribution) live on their own endpoints instead.
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    // Only ever used to parse the json_agg text that getBlockerComparison/getAchievementComparison
    // already return pre-ordered -- no JavaTimeModule needed here since these items carry no dates.
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDate weekStart, Long projectId) {
        LocalDate resolvedWeek = resolveWeekStart(weekStart);
        Instant dueAt = ComplianceDefinition.dueAt(resolvedWeek);

        List<DashboardRepository.StatusCount> statusCounts =
                dashboardRepository.getStatusCounts(resolvedWeek, projectId);
        DashboardRepository.BlockerCounts blockerCounts =
                dashboardRepository.getBlockerCounts(resolvedWeek, projectId);
        DashboardRepository.ComplianceCounts complianceCounts =
                dashboardRepository.getComplianceCounts(resolvedWeek, dueAt, projectId);

        Map<ReportStatus, Long> byStatus = new LinkedHashMap<>();
        for (DashboardRepository.StatusCount sc : statusCounts) {
            byStatus.put(sc.getStatus(), nz(sc.getCnt()));
        }

        long submitted = byStatus.getOrDefault(ReportStatus.SUBMITTED, 0L);
        long approved = byStatus.getOrDefault(ReportStatus.APPROVED, 0L);
        long needsCorrection = byStatus.getOrDefault(ReportStatus.NEEDS_CORRECTION, 0L);
        long draft = byStatus.getOrDefault(ReportStatus.DRAFT, 0L);
        long total = submitted + approved + needsCorrection + draft;

        long expected = nz(complianceCounts.getExpected());
        long onTime = nz(complianceCounts.getOnTime());
        long late = nz(complianceCounts.getLate());
        long pending = nz(complianceCounts.getPending());
        // notStartedCount counts members with zero report rows; compliance's "pending" also folds
        // in members who did start a DRAFT but never submitted it -- deliberately different metrics.
        long notStarted = Math.max(0, expected - total);

        ComplianceResponse compliance = ComplianceResponse.builder()
                .expected(expected)
                .onTime(onTime)
                .late(late)
                .pending(pending)
                .ratePercent(ComplianceDefinition.ratePercent(onTime, late, expected))
                .build();

        return DashboardSummaryResponse.builder()
                .totalReportsThisWeek(total)
                .submittedCount(submitted)
                .approvedCount(approved)
                .needsCorrectionCount(needsCorrection)
                .draftCount(draft)
                .notStartedCount(notStarted)
                .openBlockersCount(nz(blockerCounts.getOpenCount()))
                .keyBlockersCount(nz(blockerCounts.getKeyCount()))
                .compliance(compliance)
                .build();
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public StatusSummaryResponse getStatusSummary(LocalDate weekStart, Long projectId) {
        LocalDate resolvedWeek = resolveWeekStart(weekStart);

        List<DashboardRepository.StatusCount> statusCounts =
                dashboardRepository.getStatusCounts(resolvedWeek, projectId);
        List<DashboardRepository.MemberStatusCounts> memberCounts =
                dashboardRepository.getMemberStatusCounts(resolvedWeek, projectId);

        Map<String, Long> statusCountsByName = new LinkedHashMap<>();
        for (ReportStatus status : ReportStatus.values()) {
            statusCountsByName.put(status.name(), 0L);
        }
        statusCounts.forEach(sc -> statusCountsByName.put(sc.getStatus().name(), nz(sc.getCnt())));

        List<MemberStatusBreakdownResponse> members = memberCounts.stream()
                .map(m -> MemberStatusBreakdownResponse.builder()
                        .userId(m.getUserId())
                        .name(m.getName())
                        .draft(nz(m.getDraft()))
                        .submitted(nz(m.getSubmitted()))
                        .needsCorrection(nz(m.getNeedsCorrection()))
                        .approved(nz(m.getApproved()))
                        .notStarted(nz(m.getNotStarted()))
                        .build())
                .toList();

        return StatusSummaryResponse.builder()
                .statusCounts(statusCountsByName)
                .members(members)
                .build();
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public List<TaskTrendResponse> getTaskTrends(
            LocalDate startDate, LocalDate endDate, Long projectId, Long userId) {
        LocalDate[] range = resolveDateRange(startDate, endDate);

        return dashboardRepository.getTaskTrends(range[0], range[1], projectId, userId).stream()
                .map(row -> TaskTrendResponse.builder()
                        .weekStartDate(row.getWeekStartDate())
                        .completedTasks(nz(row.getCompletedTasks()))
                        .totalTasks(nz(row.getTotalTasks()))
                        .totalHoursSpent(nzBigDecimal(row.getTotalHoursSpent()))
                        .build())
                .toList();
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public List<WorkloadResponse> getWorkload(LocalDate startDate, LocalDate endDate, Long projectId) {
        LocalDate[] range = resolveDateRange(startDate, endDate);

        return dashboardRepository.getWorkload(range[0], range[1], projectId).stream()
                .map(row -> WorkloadResponse.builder()
                        .projectId(row.getProjectId())
                        .projectName(row.getProjectName())
                        .reportCount(nz(row.getReportCount()))
                        .taskCount(nz(row.getTaskCount()))
                        .totalHoursSpent(nzBigDecimal(row.getTotalHoursSpent()))
                        .memberCount(nz(row.getMemberCount()))
                        .build())
                .toList();
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public List<TimeDistributionResponse> getTimeDistribution(
            LocalDate startDate, LocalDate endDate, Long projectId) {
        LocalDate[] range = resolveDateRange(startDate, endDate);

        return dashboardRepository.getTimeDistribution(range[0], range[1], projectId).stream()
                .map(row -> TimeDistributionResponse.builder()
                        .taskType(row.getTaskType())
                        .totalHours(nzBigDecimal(row.getTotalHours()))
                        .percentOfTotal(nzBigDecimal(row.getPercentOfTotal()))
                        .build())
                .toList();
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public PagedResponse<ActivityFeedEntryResponse> getActivityFeed(Pageable pageable) {
        Page<DashboardRepository.ActivityFeedRow> rows = dashboardRepository.getActivityFeed(pageable);

        Page<ActivityFeedEntryResponse> mapped = rows.map(row -> ActivityFeedEntryResponse.builder()
                .type(row.getEventType())
                .reportId(row.getReportId())
                .weekStartDate(row.getWeekStartDate())
                .memberName(row.getMemberName())
                .projectName(row.getProjectName())
                .actorName(row.getActorName())
                .action(row.getAction())
                .comment(row.getComment())
                .occurredAt(row.getOccurredAt())
                .build());

        return PagedResponse.of(mapped);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Transactional(readOnly = true)
    public List<SectionComparisonResponse> getSectionComparison(
            LocalDate weekStart, SectionType section, Long projectId) {
        List<DashboardRepository.SectionComparisonRow> rows = section == SectionType.BLOCKERS
                ? dashboardRepository.getBlockerComparison(weekStart, projectId)
                : dashboardRepository.getAchievementComparison(weekStart, projectId);

        return rows.stream()
                .map(row -> SectionComparisonResponse.builder()
                        .userId(row.getUserId())
                        .name(row.getName())
                        .projectName(row.getProjectName())
                        .reportStatus(row.getReportStatus())
                        .items(parseItems(row.getItems()))
                        .build())
                .toList();
    }

    private List<SectionItemResponse> parseItems(String itemsJson) {
        SectionItemResponse[] items = objectMapper.readValue(itemsJson, SectionItemResponse[].class);
        return List.of(items);
    }

    private LocalDate resolveWeekStart(LocalDate weekStart) {
        if (weekStart != null) {
            return weekStart;
        }
        return LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // Shared by every range endpoint (trends/workload/time-distribution): defaults to the current
    // week (Monday-Sunday) when either bound is missing, per the module-wide filter contract.
    private LocalDate[] resolveDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return new LocalDate[] {startDate, endDate};
        }
        LocalDate currentWeekStart = resolveWeekStart(null);
        return new LocalDate[] {currentWeekStart, currentWeekStart.plusDays(6)};
    }

    private long nz(Long value) {
        return value != null ? value : 0L;
    }

    private BigDecimal nzBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
