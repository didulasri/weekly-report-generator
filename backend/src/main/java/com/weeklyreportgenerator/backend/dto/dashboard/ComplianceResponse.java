package com.weeklyreportgenerator.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceResponse {

    private long expected;
    private long onTime;
    private long late;
    private long pending;
    private double ratePercent;
}
