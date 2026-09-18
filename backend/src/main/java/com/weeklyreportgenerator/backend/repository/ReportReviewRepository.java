package com.weeklyreportgenerator.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreportgenerator.backend.entity.ReportReview;

public interface ReportReviewRepository extends JpaRepository<ReportReview, Long> {

    List<ReportReview> findByReportId(Long reportId);

    List<ReportReview> findByReviewerId(Long reviewerId);

    // Fetch-joins reviewer -- the mapper reads reviewer.getName() outside this method's transaction.
    @Query("SELECT rr FROM ReportReview rr JOIN FETCH rr.reviewer WHERE rr.report.id = :reportId "
            + "ORDER BY rr.reviewedAt DESC")
    List<ReportReview> findByReportIdOrderByReviewedAtDesc(@Param("reportId") Long reportId);

    Optional<ReportReview> findFirstByReportIdOrderByReviewedAtDesc(Long reportId);
}
