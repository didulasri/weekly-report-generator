package com.weeklyreportgenerator.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
}
