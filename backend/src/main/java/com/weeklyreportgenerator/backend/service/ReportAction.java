package com.weeklyreportgenerator.backend.service;

// Not persisted -- these are workflow verbs, not a database column.
public enum ReportAction {
    SUBMIT,
    APPROVE,
    REQUEST_CHANGES
}
