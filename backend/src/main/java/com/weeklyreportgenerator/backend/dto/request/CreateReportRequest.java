package com.weeklyreportgenerator.backend.dto.request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    @NotNull(message = "projectId is required")
    private Long projectId;

    @NotNull(message = "weekStartDate is required")
    private LocalDate weekStartDate;

    @NotNull(message = "weekEndDate is required")
    private LocalDate weekEndDate;

    private String summary;

    private String notes;

    @Valid
    private List<TaskRequest> tasks = new ArrayList<>();

    @Valid
    private List<NextWeekTaskRequest> nextWeekTasks = new ArrayList<>();

    @Valid
    private List<BlockerRequest> blockers = new ArrayList<>();

    @Valid
    private List<AchievementRequest> achievements = new ArrayList<>();

    @Valid
    private List<WorkHourRequest> workHours = new ArrayList<>();
}
