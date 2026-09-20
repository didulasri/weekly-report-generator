package com.weeklyreportgenerator.backend.dto.response;

import java.time.Instant;

import com.weeklyreportgenerator.backend.entity.enums.ProjectStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserProjectResponse {

    private Long id;
    private String name;
    private ProjectStatus status;
    private Instant assignedAt;
}
