package com.weeklyreportgenerator.backend.dto.response;

import java.time.Instant;
import java.util.List;

import com.weeklyreportgenerator.backend.entity.enums.RoleName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {

    private Long id;
    private String name;
    private String email;
    private RoleName role;
    private boolean active;
    private Instant createdAt;
    private long projectCount;
    private long reportCount;
    private List<AdminUserProjectResponse> projects;
    private ReportStatusBreakdown reportStatusBreakdown;
}
