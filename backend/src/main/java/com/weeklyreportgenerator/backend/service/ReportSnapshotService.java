package com.weeklyreportgenerator.backend.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.weeklyreportgenerator.backend.dto.response.ReportDetailResponse;
import com.weeklyreportgenerator.backend.entity.WeeklyReport;
import com.weeklyreportgenerator.backend.mapper.ReportMapper;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

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

    // Fields on ReportDetailResponse that legitimately change on every submit regardless of
    // whether report content changed -- must be excluded from the "did anything change" check.
    private static final Set<String> VOLATILE_TOP_LEVEL_FIELDS = Set.of(
            "id", "projectName", "status", "currentVersion", "submittedAt",
            "createdAt", "updatedAt", "canEdit", "reviews", "hasUnreadReview");

    public String serialize(WeeklyReport report) {
        ReportDetailResponse detail = reportMapper.toDetailResponse(report);
        return objectMapper.writeValueAsString(detail);
    }

    public ReportDetailResponse deserialize(String json) {
        return objectMapper.readValue(json, ReportDetailResponse.class);
    }

    // Compares report content against a previously stored snapshot, ignoring status/versioning
    // metadata (see VOLATILE_TOP_LEVEL_FIELDS) and every child row's database id -- PUT always
    // clears and recreates child rows with fresh ids even when the member changed nothing, so id
    // equality would produce false positives on a genuine no-op resubmit.
    public boolean contentUnchangedSince(WeeklyReport report, String previousSnapshotJson) {
        JsonNode current = normalizedContentTree(reportMapper.toDetailResponse(report));
        JsonNode previous = normalizedContentTree(deserialize(previousSnapshotJson));
        return current.equals(previous);
    }

    private JsonNode normalizedContentTree(ReportDetailResponse detail) {
        ObjectNode node = objectMapper.valueToTree(detail);
        node.remove(VOLATILE_TOP_LEVEL_FIELDS);
        stripIds(node);
        return node;
    }

    private void stripIds(JsonNode node) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            object.remove("id");
            object.properties().forEach(entry -> stripIds(entry.getValue()));
        } else if (node.isArray()) {
            node.forEach(this::stripIds);
        }
    }
}
