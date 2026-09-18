package com.weeklyreportgenerator.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.weeklyreportgenerator.backend.entity.Role;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;
import com.weeklyreportgenerator.backend.exception.InvalidStatusTransitionException;

class ReportWorkflowServiceTest {

    private final ReportWorkflowService workflowService = new ReportWorkflowService();

    private User userWithRole(long id, RoleName roleName) {
        User user = User.builder().role(Role.builder().name(roleName).build()).build();
        user.setId(id);
        return user;
    }

    private final User owner = userWithRole(1L, RoleName.TEAM_MEMBER);
    private final User manager = userWithRole(2L, RoleName.MANAGER);
    private final User admin = userWithRole(3L, RoleName.ADMIN);
    private final User otherMember = userWithRole(4L, RoleName.TEAM_MEMBER);

    private WeeklyReport reportWithStatus(ReportStatus status) {
        WeeklyReport report = WeeklyReport.builder().status(status).user(owner).build();
        report.setId(99L);
        return report;
    }

    @Test
    void draftSubmitTransitionsToSubmittedAndSetsSubmittedAt() {
        WeeklyReport report = reportWithStatus(ReportStatus.DRAFT);

        WeeklyReport result = workflowService.transition(report, ReportAction.SUBMIT, owner);

        assertThat(result.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
    }

    @Test
    void needsCorrectionSubmitTransitionsToSubmitted() {
        WeeklyReport report = reportWithStatus(ReportStatus.NEEDS_CORRECTION);

        WeeklyReport result = workflowService.transition(report, ReportAction.SUBMIT, owner);

        assertThat(result.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
    }

    @Test
    void submittingAnAlreadySubmittedReportThrows() {
        WeeklyReport report = reportWithStatus(ReportStatus.SUBMITTED);

        assertThatThrownBy(() -> workflowService.transition(report, ReportAction.SUBMIT, owner))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("SUBMIT")
                .hasMessageContaining("SUBMITTED");
    }

    @Test
    void submittingAnApprovedReportThrows() {
        WeeklyReport report = reportWithStatus(ReportStatus.APPROVED);

        assertThatThrownBy(() -> workflowService.transition(report, ReportAction.SUBMIT, owner))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("SUBMIT")
                .hasMessageContaining("APPROVED");
    }

    @Test
    void nonOwnerSubmittingThrowsAccessDenied() {
        WeeklyReport report = reportWithStatus(ReportStatus.DRAFT);

        assertThatThrownBy(() -> workflowService.transition(report, ReportAction.SUBMIT, otherMember))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void managerApprovingSubmittedReportTransitionsToApproved() {
        WeeklyReport report = reportWithStatus(ReportStatus.SUBMITTED);

        WeeklyReport result = workflowService.transition(report, ReportAction.APPROVE, manager);

        assertThat(result.getStatus()).isEqualTo(ReportStatus.APPROVED);
    }

    @Test
    void adminRequestingChangesOnSubmittedReportTransitionsToNeedsCorrection() {
        WeeklyReport report = reportWithStatus(ReportStatus.SUBMITTED);

        WeeklyReport result = workflowService.transition(report, ReportAction.REQUEST_CHANGES, admin);

        assertThat(result.getStatus()).isEqualTo(ReportStatus.NEEDS_CORRECTION);
    }

    @Test
    void teamMemberApprovingThrowsAccessDenied() {
        WeeklyReport report = reportWithStatus(ReportStatus.SUBMITTED);

        assertThatThrownBy(() -> workflowService.transition(report, ReportAction.APPROVE, owner))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void approvingDraftThrows() {
        WeeklyReport report = reportWithStatus(ReportStatus.DRAFT);

        assertThatThrownBy(() -> workflowService.transition(report, ReportAction.APPROVE, manager))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("APPROVE")
                .hasMessageContaining("DRAFT");
    }

    @Test
    void approvingAlreadyApprovedReportThrows() {
        WeeklyReport report = reportWithStatus(ReportStatus.APPROVED);

        assertThatThrownBy(() -> workflowService.transition(report, ReportAction.APPROVE, manager))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("APPROVE")
                .hasMessageContaining("APPROVED");
    }

    @Test
    void requestingChangesOnNeedsCorrectionReportThrows() {
        WeeklyReport report = reportWithStatus(ReportStatus.NEEDS_CORRECTION);

        assertThatThrownBy(() -> workflowService.transition(report, ReportAction.REQUEST_CHANGES, manager))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("REQUEST_CHANGES")
                .hasMessageContaining("NEEDS_CORRECTION");
    }

    @Test
    void assertEditableAllowsDraftAndNeedsCorrection() {
        workflowService.assertEditable(reportWithStatus(ReportStatus.DRAFT));
        workflowService.assertEditable(reportWithStatus(ReportStatus.NEEDS_CORRECTION));
    }

    @Test
    void assertEditableRejectsSubmittedAndApproved() {
        assertThatThrownBy(() -> workflowService.assertEditable(reportWithStatus(ReportStatus.SUBMITTED)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> workflowService.assertEditable(reportWithStatus(ReportStatus.APPROVED)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void assertDeletableAllowsOnlyDraft() {
        workflowService.assertDeletable(reportWithStatus(ReportStatus.DRAFT));

        assertThatThrownBy(() -> workflowService.assertDeletable(reportWithStatus(ReportStatus.NEEDS_CORRECTION)))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> workflowService.assertDeletable(reportWithStatus(ReportStatus.SUBMITTED)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
