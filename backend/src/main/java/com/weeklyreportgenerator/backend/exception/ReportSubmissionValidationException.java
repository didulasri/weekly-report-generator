package com.weeklyreportgenerator.backend.exception;

import java.util.List;

import lombok.Getter;

@Getter
public class ReportSubmissionValidationException extends RuntimeException {

    private final List<String> errors;

    public ReportSubmissionValidationException(List<String> errors) {
        super("Report is not ready for submission");
        this.errors = errors;
    }
}
