package com.weeklyreportgenerator.backend.dto.response;

import java.time.Instant;

import com.weeklyreportgenerator.backend.entity.enums.InvitationStatus;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponse {

    private Long id;
    private String email;
    private RoleName role;
    private InvitationStatus status;
    private Instant expiresAt;
    private String invitedByName;
    private Instant createdAt;
    private Instant acceptedAt;
}
