package br.com.ottonsam.toothy_planner_api.report.dtos;

import br.com.ottonsam.toothy_planner_api.report.entities.WeeklyPerformanceReportEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record WeeklyPerformanceReportResponse(
        UUID id,
        UUID calendarId,
        int week,
        LocalDate weekStartsAt,
        LocalDate weekEndsAt,
        String userFeedback,
        Map<String, Object> metrics,
        String markdownReport,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    private static final TypeReference<Map<String, Object>> METRICS_TYPE = new TypeReference<>() {};

    public WeeklyPerformanceReportResponse {
        metrics = Map.copyOf(metrics);
    }

    @Override
    public Map<String, Object> metrics() {
        return Map.copyOf(metrics);
    }

    public static WeeklyPerformanceReportResponse from(
            WeeklyPerformanceReportEntity report, ObjectMapper objectMapper) {
        try {
            return new WeeklyPerformanceReportResponse(
                    report.getId(),
                    report.getCalendar().getId(),
                    report.getWeek(),
                    report.getWeekStartsAt(),
                    report.getWeekEndsAt(),
                    report.getUserFeedback(),
                    objectMapper.readValue(report.getMetrics(), METRICS_TYPE),
                    report.getMarkdownReport(),
                    report.getCreatedAt(),
                    report.getUpdatedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored weekly report metrics must be valid JSON", exception);
        }
    }
}
