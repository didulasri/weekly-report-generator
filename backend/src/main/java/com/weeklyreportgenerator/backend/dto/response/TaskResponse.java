package com.weeklyreportgenerator.backend.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {

    private Long id;
    private String taskName;
    private String description;
    private String status;
    private String priority;
    private Integer plannedPercentage;
    private Integer actualPercentage;
    private BigDecimal hoursPlanned;
    private BigDecimal hoursSpent;
    private String deliverable;
}
