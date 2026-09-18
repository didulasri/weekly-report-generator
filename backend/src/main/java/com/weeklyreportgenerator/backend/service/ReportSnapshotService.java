package com.weeklyreportgenerator.backend.service;

import org.springframework.stereotype.Service;

import com.weeklyreportgenerator.backend.dto.response.ReportDetailResponse;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.mapper.ReportMapper;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

// Serializes a WeeklyReport aggregate to JSON (and back) for report_versions.snapshot_data.
// Always goes through ReportMapper first -- serializing the entity directly would drag in
// lazy proxies and break. Kept out of ReportWorkflowService: that class owns status rules,
// this one owns the snapshot format.
@Service
@RequiredArgsConstructor
public class ReportSnapshotService {

    private final ReportMapper reportMapper;

    // This Jackson 3 ("tools.jackson") build has JSR-310 (LocalDate/Instant) support built
    // directly into jackson-databind -- there's no separate JavaTimeModule class to register,
    // unlike classic Jackson 2. The round-trip test proves dates survive correctly regardless.
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public String serialize(WeeklyReport report) {
        ReportDetailResponse detail = reportMapper.toDetailResponse(report);
        return objectMapper.writeValueAsString(detail);
    }

    public ReportDetailResponse deserialize(String json) {
        return objectMapper.readValue(json, ReportDetailResponse.class);
    }
}
