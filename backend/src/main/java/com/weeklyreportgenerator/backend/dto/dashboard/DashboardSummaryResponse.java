package com.weeklyreportgenerator.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private long totalReportsThisWeek;
    private long submittedCount;
    private long approvedCount;
    private long needsCorrectionCount;
    private long draftCount;
    private long notStartedCount;
    private long openBlockersCount;
    private long keyBlockersCount;
    private ComplianceResponse compliance;
}
