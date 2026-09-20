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
public class TimeDistributionResponse {

    private String taskType;
    private BigDecimal totalHours;
    private BigDecimal percentOfTotal;
}
