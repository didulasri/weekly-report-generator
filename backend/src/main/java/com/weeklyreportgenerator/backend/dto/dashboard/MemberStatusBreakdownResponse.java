package com.weeklyreportgenerator.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Counts, not a single status -- a member assigned to more than one active project can be, e.g.,
// submitted on one and notStarted on the other in the same week, so each field sums across that
// member's active assignments rather than representing one exclusive state.
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberStatusBreakdownResponse {

    private Long userId;
    private String name;
    private long draft;
    private long submitted;
    private long needsCorrection;
    private long approved;
    private long notStarted;
}
