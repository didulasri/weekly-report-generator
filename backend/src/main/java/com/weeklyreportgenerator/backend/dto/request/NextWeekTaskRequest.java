package com.weeklyreportgenerator.backend.dto.request;

import com.weeklyreportgenerator.backend.entity.enums.Priority;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NextWeekTaskRequest {

    @NotBlank(message = "taskName is required")
    private String taskName;

    private String description;

    // optional: defaults to MEDIUM if omitted, matching the entity default
    private Priority priority;
}
