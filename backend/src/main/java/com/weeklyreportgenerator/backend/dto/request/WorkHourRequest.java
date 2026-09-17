package com.weeklyreportgenerator.backend.dto.request;

import java.math.BigDecimal;

import com.weeklyreportgenerator.backend.entity.enums.WorkCategory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkHourRequest {

    @NotNull(message = "taskType is required")
    private WorkCategory taskType;

    @NotNull(message = "hours is required")
    @DecimalMin(value = "0.0", message = "hours must be >= 0")
    private BigDecimal hours;
}
