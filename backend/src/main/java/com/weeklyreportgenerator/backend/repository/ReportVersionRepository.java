package com.weeklyreportgenerator.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreportgenerator.backend.entity.ReportVersion;

public interface ReportVersionRepository extends JpaRepository<ReportVersion, Long> {

    List<ReportVersion> findByReportId(Long reportId);

    List<ReportVersion> findByReportIdOrderByVersionNumberDesc(Long reportId);

    Optional<ReportVersion> findByReportIdAndVersionNumber(Long reportId, Integer versionNumber);
}
