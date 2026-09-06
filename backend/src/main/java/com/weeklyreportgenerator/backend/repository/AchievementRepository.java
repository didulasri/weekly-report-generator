package com.weeklyreportgenerator.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreportgenerator.backend.entity.Achievement;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findByReportId(Long reportId);
}
