package com.weeklyreportgenerator.backend.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTrendResponse {

    private LocalDate weekStartDate;
    private long completedTasks;
    private long totalTasks;
    private BigDecimal totalHoursSpent;
}
