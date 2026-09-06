package com.weeklyreportgenerator.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreportgenerator.backend.entity.NextWeekTask;

public interface NextWeekTaskRepository extends JpaRepository<NextWeekTask, Long> {

    List<NextWeekTask> findByReportId(Long reportId);
}
