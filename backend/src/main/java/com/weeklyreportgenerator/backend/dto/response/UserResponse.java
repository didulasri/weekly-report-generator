package com.weeklyreportgenerator.backend.dto.response;

import java.time.Instant;

import com.weeklyreportgenerator.backend.entity.enums.RoleName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private RoleName role;
    private boolean active;
    private Instant createdAt;
    private long projectCount;
    private long reportCount;
}
