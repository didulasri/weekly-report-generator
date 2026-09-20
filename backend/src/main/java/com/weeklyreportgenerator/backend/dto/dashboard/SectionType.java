package com.weeklyreportgenerator.backend.dto.dashboard;

// Not persisted -- a query-param selector for GET /api/dashboard/section-comparison, deciding
// which of the two per-report child tables (blockers or achievements) the comparison reads from.
public enum SectionType {
    BLOCKERS,
    ACHIEVEMENTS
}
