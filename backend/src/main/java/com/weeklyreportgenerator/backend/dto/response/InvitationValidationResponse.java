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
public class InvitationValidationResponse {

    private String email;
    private RoleName role;
    private Instant expiresAt;
}
