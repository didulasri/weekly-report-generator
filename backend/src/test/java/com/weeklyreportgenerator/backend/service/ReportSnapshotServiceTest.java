package com.weeklyreportgenerator.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.weeklyreportgenerator.backend.dto.response.ReportDetailResponse;
import com.weeklyreportgenerator.backend.entity.Achievement;
import com.weeklyreportgenerator.backend.entity.Blocker;
import com.weeklyreportgenerator.backend.entity.NextWeekTask;
import com.weeklyreportgenerator.backend.entity.Project;
import com.weeklyreportgenerator.backend.entity.ReportTask;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.WorkHour;
import com.weeklyreportgenerator.backend.entity.enums.BlockerStatus;
import com.weeklyreportgenerator.backend.entity.enums.ImpactLevel;
import com.weeklyreportgenerator.backend.entity.enums.Priority;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;
import com.weeklyreportgenerator.backend.entity.enums.TaskStatus;
import com.weeklyreportgenerator.backend.entity.enums.WorkCategory;
import com.weeklyreportgenerator.backend.mapper.ReportMapper;

class ReportSnapshotServiceTest {

    private final ReportSnapshotService snapshotService = new ReportSnapshotService(new ReportMapper());

    @Test
    void serializeThenDeserializeRoundTripsAllFieldsIncludingDates() {
        Project project = Project.builder().name("Client A").build();
        project.setId(1L);

        WeeklyReport report = WeeklyReport.builder()
                .project(project)
                .weekStartDate(LocalDate.of(2027, 1, 4))
                .weekEndDate(LocalDate.of(2027, 1, 10))
                .status(ReportStatus.SUBMITTED)
                .summary("Weekly summary")
                .notes("Some notes")
                .currentVersion(1)
                .build();
        report.setId(42L);
        report.setCreatedAt(Instant.parse("2027-01-04T09:00:00Z"));
        report.setUpdatedAt(Instant.parse("2027-01-10T17:30:00Z"));

        report.addTask(ReportTask.builder()
                .taskName("Task 1")
                .status(TaskStatus.IN_PROGRESS)
                .priority(Priority.HIGH)
                .plannedPercentage(100)
                .actualPercentage(50)
                .hoursPlanned(new BigDecimal("8.00"))
                .hoursSpent(new BigDecimal("4.00"))
                .build());

        report.addNextWeekTask(NextWeekTask.builder()
                .taskName("Next task")
                .priority(Priority.MEDIUM)
                .build());

        report.addBlocker(Blocker.builder()
                .title("Blocker 1")
                .impact(ImpactLevel.HIGH)
                .status(BlockerStatus.OPEN)
                .isKeyIssue(true)
                .build());

        report.addAchievement(Achievement.builder()
                .title("Achievement 1")
                .isKeyAchievement(true)
                .build());

        report.addWorkHour(WorkHour.builder()
                .taskType(WorkCategory.DEVELOPMENT)
                .hours(new BigDecimal("10.00"))
                .build());

        String json = snapshotService.serialize(report);
        ReportDetailResponse roundTripped = snapshotService.deserialize(json);

        assertThat(roundTripped.getId()).isEqualTo(42L);
        assertThat(roundTripped.getProjectId()).isEqualTo(1L);
        assertThat(roundTripped.getProjectName()).isEqualTo("Client A");
        assertThat(roundTripped.getWeekStartDate()).isEqualTo(LocalDate.of(2027, 1, 4));
        assertThat(roundTripped.getWeekEndDate()).isEqualTo(LocalDate.of(2027, 1, 10));
        assertThat(roundTripped.getStatus()).isEqualTo("SUBMITTED");
        assertThat(roundTripped.getSummary()).isEqualTo("Weekly summary");
        assertThat(roundTripped.getNotes()).isEqualTo("Some notes");
        assertThat(roundTripped.getCurrentVersion()).isEqualTo(1);
        assertThat(roundTripped.getCreatedAt()).isEqualTo(Instant.parse("2027-01-04T09:00:00Z"));
        assertThat(roundTripped.getUpdatedAt()).isEqualTo(Instant.parse("2027-01-10T17:30:00Z"));

        assertThat(roundTripped.getTasks()).hasSize(1);
        assertThat(roundTripped.getTasks().get(0).getTaskName()).isEqualTo("Task 1");
        assertThat(roundTripped.getTasks().get(0).getHoursPlanned()).isEqualByComparingTo(new BigDecimal("8.00"));

        assertThat(roundTripped.getNextWeekTasks()).hasSize(1);
        assertThat(roundTripped.getNextWeekTasks().get(0).getTaskName()).isEqualTo("Next task");

        assertThat(roundTripped.getBlockers()).hasSize(1);
        assertThat(roundTripped.getBlockers().get(0).isKeyIssue()).isTrue();

        assertThat(roundTripped.getAchievements()).hasSize(1);
        assertThat(roundTripped.getAchievements().get(0).isKeyAchievement()).isTrue();

        assertThat(roundTripped.getWorkHours()).hasSize(1);
        assertThat(roundTripped.getWorkHours().get(0).getHours()).isEqualByComparingTo(new BigDecimal("10.00"));
    }
}
