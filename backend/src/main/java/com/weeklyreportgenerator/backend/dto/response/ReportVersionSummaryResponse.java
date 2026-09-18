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
public class ReportVersionSummaryResponse {

    private Integer versionNumber;
    private Instant submittedAt;
}
