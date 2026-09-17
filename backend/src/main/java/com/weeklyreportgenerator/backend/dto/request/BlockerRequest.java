package com.weeklyreportgenerator.backend.dto.request;

import com.weeklyreportgenerator.backend.entity.enums.BlockerStatus;
import com.weeklyreportgenerator.backend.entity.enums.ImpactLevel;

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
public class BlockerRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    @NotNull(message = "impact is required")
    private ImpactLevel impact;

    // optional: defaults to OPEN if omitted, matching the entity default
    private BlockerStatus status;

    private boolean keyIssue;
}
