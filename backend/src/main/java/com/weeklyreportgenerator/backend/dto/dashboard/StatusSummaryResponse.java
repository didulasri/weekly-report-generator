package com.weeklyreportgenerator.backend.dto.dashboard;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusSummaryResponse {

    // Keyed by ReportStatus name, always all four keys present (zero-filled), never a sparse map.
    private Map<String, Long> statusCounts;
    private List<MemberStatusBreakdownResponse> members;
}
