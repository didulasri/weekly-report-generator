package com.weeklyreportgenerator.backend.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDetailResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String status;
    private String summary;
    private String notes;
    private Integer currentVersion;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean canEdit;

    private List<TaskResponse> tasks;
    private List<NextWeekTaskResponse> nextWeekTasks;
    private List<BlockerResponse> blockers;
    private List<AchievementResponse> achievements;
    private List<WorkHourResponse> workHours;

    // Newest first. Carries reviewerName only -- never the reviewer's email, so a member can see
    // why their report came back without seeing anything else about the reviewer.
    private List<ReviewResponse> reviews;
    private boolean hasUnreadReview;
}
