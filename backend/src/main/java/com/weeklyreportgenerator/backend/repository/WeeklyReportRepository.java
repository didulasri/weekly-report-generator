package com.weeklyreportgenerator.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.entity.enums.ReportStatus;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long>, JpaSpecificationExecutor<WeeklyReport> {

    Optional<WeeklyReport> findByIdAndUserId(Long id, Long userId);

    List<WeeklyReport> findByUserId(Long userId);

    List<WeeklyReport> findByStatus(ReportStatus status);

    List<WeeklyReport> findByProjectId(Long projectId);

    boolean existsByUserIdAndProjectIdAndWeekStartDate(Long userId, Long projectId, LocalDate weekStartDate);

    boolean existsByUserIdAndProjectIdAndWeekStartDateAndIdNot(
            Long userId, Long projectId, LocalDate weekStartDate, Long id);

    // One grouped query for every team member on the page, instead of one count query per member.
    @Query("SELECT r.user.id AS userId, r.status AS status, COUNT(r) AS cnt "
            + "FROM WeeklyReport r WHERE r.user.id IN :userIds GROUP BY r.user.id, r.status")
    List<ReportStatusCount> countByStatusGroupedByUser(@Param("userIds") List<Long> userIds);

    interface ReportStatusCount {
        Long getUserId();
        ReportStatus getStatus();
        Long getCnt();
    }
}
