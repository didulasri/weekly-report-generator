package com.weeklyreportgenerator.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

// Single source of truth for the compliance business rule (assignment section 6 / dashboard spec).
// Every dashboard query that classifies a report as on-time/late/pending goes through this class
// so the definition can never drift between the summary, status-summary, and any future endpoint.
//
// Rule:
//   - A report for the week starting Monday W is DUE at 23:59:59 UTC on the Monday of week W+1.
//   - ON_TIME: the report exists, status is anything other than DRAFT, and submittedAt <= due date.
//   - LATE:    the report exists, status is anything other than DRAFT, and submittedAt >  due date.
//   - PENDING: the member is assigned to the project but has no report row for that week, or the
//              report exists but is still DRAFT (DRAFT never counts as submitted, however early).
//   - Compliance rate = (onTime + late) / expected, where expected is the count of active members
//     assigned to at least one active project for that week. Zero expected reports a 0% rate, never
//     a division by zero.
//
// "Assigned ... for that week" is read as "currently has an active user_projects assignment" --
// the schema has no per-week assignment history, so there is no other week-scoped signal to use.
public final class ComplianceDefinition {

    private ComplianceDefinition() {
    }

    public static Instant dueAt(LocalDate weekStartDate) {
        return weekStartDate.plusWeeks(1).atTime(LocalTime.of(23, 59, 59)).toInstant(ZoneOffset.UTC);
    }

    public static double ratePercent(long onTime, long late, long expected) {
        if (expected <= 0) {
            return 0.0;
        }
        return Math.round(((onTime + late) * 10000.0) / expected) / 100.0;
    }
}
