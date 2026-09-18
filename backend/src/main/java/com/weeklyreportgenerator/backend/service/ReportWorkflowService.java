package com.weeklyreportgenerator.backend.service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;
import com.weeklyreportgenerator.backend.exception.InvalidStatusTransitionException;

// The single authority for WeeklyReport status changes -- every caller, current and future, goes
// through transition(). Adding a new action means growing LEGAL_ACTIONS, resolveNextStatus(), and
// assertActorAuthorized(); nothing else needs to change.
@Service
public class ReportWorkflowService {

    private static final Map<ReportStatus, Set<ReportAction>> LEGAL_ACTIONS = Map.of(
            ReportStatus.DRAFT, Set.of(ReportAction.SUBMIT),
            ReportStatus.NEEDS_CORRECTION, Set.of(ReportAction.SUBMIT),
            ReportStatus.SUBMITTED, Set.of(ReportAction.APPROVE, ReportAction.REQUEST_CHANGES));

    public WeeklyReport transition(WeeklyReport report, ReportAction action, User actor) {
        ReportStatus currentStatus = report.getStatus();
        Set<ReportAction> allowedActions = LEGAL_ACTIONS.getOrDefault(currentStatus, Set.of());

        if (!allowedActions.contains(action)) {
            throw new InvalidStatusTransitionException(
                    "Cannot " + action + " report " + report.getId() + " while it is " + currentStatus);
        }

        assertActorAuthorized(report, action, actor);

        ReportStatus nextStatus = resolveNextStatus(action);
        Instant submittedAt = action == ReportAction.SUBMIT ? Instant.now() : null;
        report.applyStatusChange(nextStatus, submittedAt);

        return report;
    }

    // Belt-and-suspenders alongside @PreAuthorize on the calling service methods: this is the one
    // place the rule lives, so no future caller of transition() can skip it.
    private void assertActorAuthorized(WeeklyReport report, ReportAction action, User actor) {
        switch (action) {
            case SUBMIT -> {
                if (!actor.getId().equals(report.getUser().getId())) {
                    throw new AccessDeniedException("Only the report owner may submit this report");
                }
            }
            case APPROVE, REQUEST_CHANGES -> {
                RoleName role = actor.getRole().getName();
                if (role != RoleName.MANAGER && role != RoleName.ADMIN) {
                    throw new AccessDeniedException("Only a manager or admin may " + action + " a report");
                }
            }
        }
    }

    private ReportStatus resolveNextStatus(ReportAction action) {
        return switch (action) {
            case SUBMIT -> ReportStatus.SUBMITTED;
            case APPROVE -> ReportStatus.APPROVED;
            case REQUEST_CHANGES -> ReportStatus.NEEDS_CORRECTION;
        };
    }

    public void assertEditable(WeeklyReport report) {
        ReportStatus status = report.getStatus();
        if (status != ReportStatus.DRAFT && status != ReportStatus.NEEDS_CORRECTION) {
            throw new InvalidStatusTransitionException(
                    "Report " + report.getId() + " cannot be edited while in status " + status);
        }
    }

    public void assertDeletable(WeeklyReport report) {
        if (report.getStatus() != ReportStatus.DRAFT) {
            throw new InvalidStatusTransitionException(
                    "Report " + report.getId() + " can only be deleted while in status DRAFT");
        }
    }
}
