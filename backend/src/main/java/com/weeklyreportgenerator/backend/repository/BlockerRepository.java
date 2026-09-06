package com.weeklyreportgenerator.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreportgenerator.backend.entity.Blocker;
import com.weeklyreportgenerator.backend.entity.enums.BlockerStatus;

public interface BlockerRepository extends JpaRepository<Blocker, Long> {

    List<Blocker> findByReportId(Long reportId);

    List<Blocker> findByStatus(BlockerStatus status);
}
