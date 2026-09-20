package com.weeklyreportgenerator.backend.dto.dashboard;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkloadResponse {

    private Long projectId;
    private String projectName;
    private long reportCount;
    private long taskCount;
    private BigDecimal totalHoursSpent;
    private long memberCount;
}
