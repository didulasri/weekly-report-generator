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
public class WorkHourResponse {

    private Long id;
    private String taskType;
    private BigDecimal hours;
}
