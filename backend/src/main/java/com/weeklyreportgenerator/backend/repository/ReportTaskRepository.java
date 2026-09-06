package com.weeklyreportgenerator.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreportgenerator.backend.entity.ReportTask;

public interface ReportTaskRepository extends JpaRepository<ReportTask, Long> {

    List<ReportTask> findByReportId(Long reportId);
}
