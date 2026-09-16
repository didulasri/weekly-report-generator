package com.weeklyreportgenerator.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private UserSummaryResponse user;
}
