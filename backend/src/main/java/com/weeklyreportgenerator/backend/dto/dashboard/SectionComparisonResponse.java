package com.weeklyreportgenerator.backend.dto.dashboard;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionComparisonResponse {

    private Long userId;
    private String name;
    private String projectName;
    // DRAFT / SUBMITTED / NEEDS_CORRECTION / APPROVED / NOT_STARTED (member has no report for the week)
    private String reportStatus;
    // Key-flagged item first, per the requirement; empty (never null) when there's no report or no items.
    private List<SectionItemResponse> items;
}
