package com.weeklyreportgenerator.backend.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreportgenerator.backend.entity.WorkHour;

public interface WorkHourRepository extends JpaRepository<WorkHour, Long> {

    List<WorkHour> findByReportId(Long reportId);

    @Query("SELECT COALESCE(SUM(w.hours), 0) FROM WorkHour w WHERE w.report.id = :reportId")
    BigDecimal sumHoursByReportId(@Param("reportId") Long reportId);
}
