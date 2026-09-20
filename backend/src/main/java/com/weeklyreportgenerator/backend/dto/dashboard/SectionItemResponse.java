package com.weeklyreportgenerator.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// One shape for both blocker and achievement items -- impact/status are populated for BLOCKERS
// only and stay null for ACHIEVEMENTS, since achievements have no equivalent fields.
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionItemResponse {

    private Long id;
    private String title;
    private String description;
    private String impact;
    private String status;
    private boolean keyFlag;
}
