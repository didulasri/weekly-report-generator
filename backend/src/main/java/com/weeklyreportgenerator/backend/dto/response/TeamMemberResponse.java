package com.weeklyreportgenerator.backend.dto.response;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberResponse {

    private Long id;
    private String name;
    private String email;
    private boolean active;
    private Map<String, Long> reportCountsByStatus;
}
