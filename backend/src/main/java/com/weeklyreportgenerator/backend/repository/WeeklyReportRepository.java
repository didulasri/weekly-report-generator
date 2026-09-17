package com.weeklyreportgenerator.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
