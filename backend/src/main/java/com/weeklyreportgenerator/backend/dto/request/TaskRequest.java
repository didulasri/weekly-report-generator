package com.weeklyreportgenerator.backend.dto.request;

import java.math.BigDecimal;

import com.weeklyreportgenerator.backend.entity.enums.Priority;
import com.weeklyreportgenerator.backend.entity.enums.TaskStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank(message = "taskName is required")
    private String taskName;

    private String description;

    @NotNull(message = "status is required")
    private TaskStatus status;

    @NotNull(message = "priority is required")
    private Priority priority;

    @NotNull(message = "plannedPercentage is required")
    @Min(value = 0, message = "plannedPercentage must be between 0 and 100")
    @Max(value = 100, message = "plannedPercentage must be between 0 and 100")
    private Integer plannedPercentage;

    @NotNull(message = "actualPercentage is required")
    @Min(value = 0, message = "actualPercentage must be between 0 and 100")
    @Max(value = 100, message = "actualPercentage must be between 0 and 100")
    private Integer actualPercentage;

    @NotNull(message = "hoursPlanned is required")
    @DecimalMin(value = "0.0", message = "hoursPlanned must be >= 0")
    private BigDecimal hoursPlanned;

    @NotNull(message = "hoursSpent is required")
    @DecimalMin(value = "0.0", message = "hoursSpent must be >= 0")
    private BigDecimal hoursSpent;

    private String deliverable;
}
