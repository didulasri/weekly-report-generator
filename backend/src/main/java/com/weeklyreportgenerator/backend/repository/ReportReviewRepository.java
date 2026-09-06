package com.weeklyreportgenerator.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreportgenerator.backend.entity.ReportReview;

public interface ReportReviewRepository extends JpaRepository<ReportReview, Long> {

    List<ReportReview> findByReportId(Long reportId);

    List<ReportReview> findByReviewerId(Long reviewerId);
}
