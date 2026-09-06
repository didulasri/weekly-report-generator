package com.weeklyreportgenerator.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreportgenerator.backend.entity.WorkHour;

public interface WorkHourRepository extends JpaRepository<WorkHour, Long> {

    List<WorkHour> findByReportId(Long reportId);
}
