package com.weeklyreportgenerator.backend.dto.response;

import java.math.BigDecimal;
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
public class ReportSummaryResponse {

    private Long id;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String projectName;
    private String status;
    private long taskCount;
    private BigDecimal totalHours;
    private Instant updatedAt;
}
