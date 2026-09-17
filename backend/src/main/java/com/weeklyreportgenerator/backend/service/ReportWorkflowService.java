package com.weeklyreportgenerator.backend.service;

import org.springframework.stereotype.Service;

import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;
import com.weeklyreportgenerator.backend.exception.InvalidStatusTransitionException;

// Submit/approve/request-changes transitions land in the review step; only edit/delete guards for now.
@Service
public class ReportWorkflowService {

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
