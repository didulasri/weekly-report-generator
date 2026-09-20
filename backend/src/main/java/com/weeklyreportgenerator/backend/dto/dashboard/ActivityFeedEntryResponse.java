package com.weeklyreportgenerator.backend.dto.dashboard;

import java.time.Instant;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityFeedEntryResponse {

    // "SUBMISSION" or "REVIEW"
    private String type;
    private Long reportId;
    private LocalDate weekStartDate;
    private String memberName;
    private String projectName;
    private String actorName;
    // SUBMITTED, APPROVED, or REQUEST_CHANGES
    private String action;
    private String comment;
    private Instant occurredAt;
}
