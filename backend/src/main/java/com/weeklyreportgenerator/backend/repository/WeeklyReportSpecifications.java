package com.weeklyreportgenerator.backend.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;

import jakarta.persistence.criteria.JoinType;

public final class WeeklyReportSpecifications {

    private WeeklyReportSpecifications() {
    }

    public static Specification<WeeklyReport> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    // fetch-joins project so summary mapping can read the name without a per-row lazy load; skipped on the count query
    public static Specification<WeeklyReport> fetchProject() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("project", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    public static Specification<WeeklyReport> hasStatus(ReportStatus status) {
        if (status == null) {
            return noop();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<WeeklyReport> hasProjectId(Long projectId) {
        if (projectId == null) {
            return noop();
        }
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<WeeklyReport> weekStartsOnOrAfter(LocalDate startDate) {
        if (startDate == null) {
            return noop();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("weekStartDate"), startDate);
    }

    public static Specification<WeeklyReport> weekStartsOnOrBefore(LocalDate endDate) {
        if (endDate == null) {
            return noop();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("weekStartDate"), endDate);
    }

    // Spring Data JPA 4.x's Specification.and() rejects a literal null, unlike older versions -- return a
    // conjunction (always-true) predicate instead when a filter is absent.
    private static Specification<WeeklyReport> noop() {
        return (root, query, cb) -> cb.conjunction();
    }
}
