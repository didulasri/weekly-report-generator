package com.weeklyreportgenerator.backend.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.weeklyreportgenerator.backend.dto.response.AchievementResponse;
import com.weeklyreportgenerator.backend.dto.response.BlockerResponse;
import com.weeklyreportgenerator.backend.dto.response.NextWeekTaskResponse;
import com.weeklyreportgenerator.backend.dto.response.ReportDetailResponse;
import com.weeklyreportgenerator.backend.dto.response.ReportSummaryResponse;
import com.weeklyreportgenerator.backend.dto.response.TaskResponse;
import com.weeklyreportgenerator.backend.dto.response.WorkHourResponse;
import com.weeklyreportgenerator.backend.entity.Achievement;
import com.weeklyreportgenerator.backend.entity.Blocker;
import com.weeklyreportgenerator.backend.entity.NextWeekTask;
import com.weeklyreportgenerator.backend.entity.ReportTask;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.WorkHour;

@Component
public class ReportMapper {

    public ReportDetailResponse toDetailResponse(WeeklyReport report) {
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
                .tasks(report.getTasks().stream().map(this::toTaskResponse).toList())
                .nextWeekTasks(report.getNextWeekTasks().stream().map(this::toNextWeekTaskResponse).toList())
                .blockers(report.getBlockers().stream().map(this::toBlockerResponse).toList())
                .achievements(report.getAchievements().stream().map(this::toAchievementResponse).toList())
                .workHours(report.getWorkHours().stream().map(this::toWorkHourResponse).toList())
                .build();
    }

    public ReportSummaryResponse toSummaryResponse(WeeklyReport report, long taskCount, BigDecimal totalHours) {
        return ReportSummaryResponse.builder()
                .id(report.getId())
                .weekStartDate(report.getWeekStartDate())
                .weekEndDate(report.getWeekEndDate())
                .projectName(report.getProject().getName())
                .status(report.getStatus().name())
                .taskCount(taskCount)
                .totalHours(totalHours)
                .updatedAt(report.getUpdatedAt())
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
