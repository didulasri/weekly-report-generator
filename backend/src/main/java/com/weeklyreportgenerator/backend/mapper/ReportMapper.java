package com.weeklyreportgenerator.backend.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.weeklyreportgenerator.backend.dto.response.AchievementResponse;
import com.weeklyreportgenerator.backend.dto.response.BlockerResponse;
import com.weeklyreportgenerator.backend.dto.response.ManagerReportDetailResponse;
import com.weeklyreportgenerator.backend.dto.response.ManagerReportSummaryResponse;
import com.weeklyreportgenerator.backend.dto.response.NextWeekTaskResponse;
import com.weeklyreportgenerator.backend.dto.response.ReportDetailResponse;
import com.weeklyreportgenerator.backend.dto.response.ReportSummaryResponse;
import com.weeklyreportgenerator.backend.dto.response.ReportVersionSummaryResponse;
import com.weeklyreportgenerator.backend.dto.response.ReviewResponse;
import com.weeklyreportgenerator.backend.dto.response.TaskResponse;
import com.weeklyreportgenerator.backend.dto.response.WorkHourResponse;
import com.weeklyreportgenerator.backend.entity.Achievement;
import com.weeklyreportgenerator.backend.entity.Blocker;
import com.weeklyreportgenerator.backend.entity.NextWeekTask;
import com.weeklyreportgenerator.backend.entity.ReportReview;
import com.weeklyreportgenerator.backend.entity.ReportTask;
import com.weeklyreportgenerator.backend.entity.ReportVersion;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.WorkHour;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;

@Component
public class ReportMapper {

    public ReportDetailResponse toDetailResponse(WeeklyReport report) {
        return toDetailResponse(report, List.of());
    }

    // reviews defaults to empty for the version-snapshot path (ReportSnapshotService) -- a snapshot
    // captures report content as of submission, not the review history that came after it.
    public ReportDetailResponse toDetailResponse(WeeklyReport report, List<ReportReview> reviews) {
        return ReportDetailResponse.builder()
                .id(report.getId())
                .projectId(report.getProject().getId())
                .projectName(report.getProject().getName())
                .weekStartDate(report.getWeekStartDate())
                .weekEndDate(report.getWeekEndDate())
                .status(report.getStatus().name())
                .summary(report.getSummary())
                .notes(report.getNotes())
                .currentVersion(report.getCurrentVersion())
                .submittedAt(report.getSubmittedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .canEdit(isEditable(report.getStatus()))
                .tasks(report.getTasks().stream().map(this::toTaskResponse).toList())
                .nextWeekTasks(report.getNextWeekTasks().stream().map(this::toNextWeekTaskResponse).toList())
                .blockers(report.getBlockers().stream().map(this::toBlockerResponse).toList())
                .achievements(report.getAchievements().stream().map(this::toAchievementResponse).toList())
                .workHours(report.getWorkHours().stream().map(this::toWorkHourResponse).toList())
                .reviews(reviews.stream().map(this::toReviewResponse).toList())
                .build();
    }

    public ReportSummaryResponse toSummaryResponse(WeeklyReport report, long taskCount, BigDecimal totalHours) {
        return toSummaryResponse(report, taskCount, totalHours, null);
    }

    public ReportSummaryResponse toSummaryResponse(
            WeeklyReport report, long taskCount, BigDecimal totalHours, String latestReviewComment) {
        return ReportSummaryResponse.builder()
                .id(report.getId())
                .weekStartDate(report.getWeekStartDate())
                .weekEndDate(report.getWeekEndDate())
                .projectName(report.getProject().getName())
                .status(report.getStatus().name())
                .taskCount(taskCount)
                .totalHours(totalHours)
                .updatedAt(report.getUpdatedAt())
                .canEdit(isEditable(report.getStatus()))
                .latestReviewComment(latestReviewComment)
                .build();
    }

    // Mirrors ReportWorkflowService.assertEditable's rule (DRAFT/NEEDS_CORRECTION only), duplicated
    // here as a read-only projection so the frontend doesn't have to reimplement the status rules.
    private boolean isEditable(ReportStatus status) {
        return status == ReportStatus.DRAFT || status == ReportStatus.NEEDS_CORRECTION;
    }

    public ReportVersionSummaryResponse toVersionSummaryResponse(ReportVersion version) {
        return ReportVersionSummaryResponse.builder()
                .versionNumber(version.getVersionNumber())
                .submittedAt(version.getSubmittedAt())
                .build();
    }

    public ManagerReportSummaryResponse toManagerSummaryResponse(
            WeeklyReport report, long taskCount, BigDecimal totalHours) {
        return ManagerReportSummaryResponse.builder()
                .report(toSummaryResponse(report, taskCount, totalHours))
                .ownerName(report.getUser().getName())
                .ownerEmail(report.getUser().getEmail())
                .build();
    }

    public ManagerReportDetailResponse toManagerDetailResponse(
            WeeklyReport report, List<ReportReview> reviews, List<ReportVersion> versions) {
        return ManagerReportDetailResponse.builder()
                .report(toDetailResponse(report))
                .ownerName(report.getUser().getName())
                .ownerEmail(report.getUser().getEmail())
                .reviews(reviews.stream().map(this::toReviewResponse).toList())
                .versions(versions.stream().map(this::toVersionSummaryResponse).toList())
                .build();
    }

    public ReviewResponse toReviewResponse(ReportReview review) {
        return ReviewResponse.builder()
                .action(review.getAction().name())
                .comment(review.getComment())
                .versionNumber(review.getVersionNumber())
                .reviewedAt(review.getReviewedAt())
                .reviewerName(review.getReviewer().getName())
                .build();
    }

    private TaskResponse toTaskResponse(ReportTask task) {
        return TaskResponse.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .plannedPercentage(task.getPlannedPercentage())
                .actualPercentage(task.getActualPercentage())
                .hoursPlanned(task.getHoursPlanned())
                .hoursSpent(task.getHoursSpent())
                .deliverable(task.getDeliverable())
                .build();
    }

    private NextWeekTaskResponse toNextWeekTaskResponse(NextWeekTask task) {
        return NextWeekTaskResponse.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .priority(task.getPriority().name())
                .build();
    }

    private BlockerResponse toBlockerResponse(Blocker blocker) {
        return BlockerResponse.builder()
                .id(blocker.getId())
                .title(blocker.getTitle())
                .description(blocker.getDescription())
                .impact(blocker.getImpact().name())
                .status(blocker.getStatus().name())
                .keyIssue(blocker.isKeyIssue())
                .build();
    }

    private AchievementResponse toAchievementResponse(Achievement achievement) {
        return AchievementResponse.builder()
                .id(achievement.getId())
                .title(achievement.getTitle())
                .description(achievement.getDescription())
                .keyAchievement(achievement.isKeyAchievement())
                .build();
    }

    private WorkHourResponse toWorkHourResponse(WorkHour workHour) {
        return WorkHourResponse.builder()
                .id(workHour.getId())
                .taskType(workHour.getTaskType().name())
                .hours(workHour.getHours())
                .build();
    }
}
