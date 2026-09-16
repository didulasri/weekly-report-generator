package com.weeklyreportgenerator.backend.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberResponse {

    private Long userId;
    private String name;
    private String email;
    private String role;
    private Instant assignedAt;
}
