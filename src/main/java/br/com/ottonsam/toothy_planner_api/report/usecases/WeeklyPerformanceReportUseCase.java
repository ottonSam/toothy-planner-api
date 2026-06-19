package br.com.ottonsam.toothy_planner_api.report.usecases;

import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityProgressCountEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityProgressDayEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityProgressTimeEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityType;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityProgressCountRepository;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityProgressDayRepository;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityProgressTimeRepository;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityRepository;
import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.calendar.entities.CalendarEntity;
import br.com.ottonsam.toothy_planner_api.calendar.repositories.CalendarRepository;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.report.dtos.WeeklyPerformanceReportRequest;
import br.com.ottonsam.toothy_planner_api.report.dtos.WeeklyPerformanceReportResponse;
import br.com.ottonsam.toothy_planner_api.report.entities.WeeklyPerformanceReportEntity;
import br.com.ottonsam.toothy_planner_api.report.repositories.WeeklyPerformanceReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WeeklyPerformanceReportUseCase {

    private final WeeklyPerformanceReportRepository reportRepository;
    private final CalendarRepository calendarRepository;
    private final ActivityRepository activityRepository;
    private final ActivityProgressDayRepository dayRepository;
    private final ActivityProgressCountRepository countRepository;
    private final ActivityProgressTimeRepository timeRepository;
    private final CurrentUserProvider currentUserProvider;
    private final WeeklyReportAiClient aiClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public WeeklyPerformanceReportUseCase(
            WeeklyPerformanceReportRepository reportRepository,
            CalendarRepository calendarRepository,
            ActivityRepository activityRepository,
            ActivityProgressDayRepository dayRepository,
            ActivityProgressCountRepository countRepository,
            ActivityProgressTimeRepository timeRepository,
            CurrentUserProvider currentUserProvider,
            WeeklyReportAiClient aiClient,
            ObjectMapper objectMapper,
            Clock clock) {
        this.reportRepository = reportRepository;
        this.calendarRepository = calendarRepository;
        this.activityRepository = activityRepository;
        this.dayRepository = dayRepository;
        this.countRepository = countRepository;
        this.timeRepository = timeRepository;
        this.currentUserProvider = currentUserProvider;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public WeeklyPerformanceReportResponse create(UUID calendarId, int week, WeeklyPerformanceReportRequest request) {
        var user = currentUserProvider.get();
        var calendar = findOwnedCalendar(calendarId, user.getId());
        WeeklyPerformanceReportEntity.validateWeek(week, calendar.getWeeks());
        var userFeedback = request == null ? null : request.userFeedback();
        WeeklyPerformanceReportEntity.validateUserFeedback(userFeedback);
        var requiredUserFeedback = Objects.requireNonNull(userFeedback);
        var weekStartsAt = weekStartsAt(calendar, week);
        var weekEndsAt = weekStartsAt.plusDays(6);
        validateGenerationDate(weekEndsAt);
        validateNotDuplicate(calendarId, week);
        var metrics = buildMetrics(calendar, week, weekStartsAt, weekEndsAt);
        var previousReports = previousReports(calendarId, week);
        var markdown = aiClient.generate(buildPrompt(metrics, previousReports, requiredUserFeedback));
        var report = WeeklyPerformanceReportEntity.create(
                calendar, week, weekStartsAt, weekEndsAt, requiredUserFeedback, toJson(metrics), markdown);
        return WeeklyPerformanceReportResponse.from(reportRepository.save(report), objectMapper);
    }

    @Transactional(readOnly = true)
    public WeeklyPerformanceReportResponse get(UUID calendarId, int week) {
        var user = currentUserProvider.get();
        return WeeklyPerformanceReportResponse.from(
                reportRepository
                        .findByCalendarIdAndWeekAndCalendarUserId(calendarId, week, user.getId())
                        .orElseThrow(
                                () -> new ApiException(HttpStatus.NOT_FOUND, "Weekly performance report not found")),
                objectMapper);
    }

    @Transactional(readOnly = true)
    public List<WeeklyPerformanceReportResponse> list(UUID calendarId) {
        var user = currentUserProvider.get();
        findOwnedCalendar(calendarId, user.getId());
        return reportRepository.findAllByCalendarIdAndCalendarUserIdOrderByWeekAsc(calendarId, user.getId()).stream()
                .map(report -> WeeklyPerformanceReportResponse.from(report, objectMapper))
                .toList();
    }

    private CalendarEntity findOwnedCalendar(UUID calendarId, UUID userId) {
        if (calendarId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Calendar id is required");
        }
        return calendarRepository
                .findByIdAndUserId(calendarId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Calendar not found"));
    }

    private void validateGenerationDate(LocalDate weekEndsAt) {
        if (LocalDate.now(clock).isBefore(weekEndsAt)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Weekly performance report can only be generated on or after the week end date");
        }
    }

    private void validateNotDuplicate(UUID calendarId, int week) {
        if (reportRepository.existsByCalendarIdAndWeek(calendarId, week)) {
            throw new ApiException(HttpStatus.CONFLICT, "Weekly performance report already exists for this week");
        }
    }

    private Map<String, Object> buildMetrics(
            CalendarEntity calendar, int week, LocalDate weekStartsAt, LocalDate weekEndsAt) {
        var activities = activityRepository.findAllByCalendarIdAndWeekOrderByCreatedAtAsc(calendar.getId(), week);
        var activityMetrics = new ArrayList<Map<String, Object>>();
        var expectedTotal = 0;
        var deliveredTotal = 0;
        var deliveryPercentageTotal = 0.0;
        for (var activity : activities) {
            var metric = activityMetric(activity, weekStartsAt, weekEndsAt);
            activityMetrics.add(metric);
            expectedTotal += activity.getGoal();
            deliveredTotal += (int) metric.get("delivered");
            deliveryPercentageTotal += (double) metric.get("deliveryPercentage");
        }

        var metrics = new LinkedHashMap<String, Object>();
        metrics.put("calendarId", calendar.getId().toString());
        metrics.put("calendarDescription", calendar.getDescription());
        metrics.put("week", week);
        metrics.put("weekStartsAt", weekStartsAt.toString());
        metrics.put("weekEndsAt", weekEndsAt.toString());
        metrics.put("weekStartsOn", calendar.getWeekStartsOn().name());
        metrics.put("weekEndsOn", calendar.getWeekEndsOn().name());
        metrics.put("totalActivities", activities.size());
        metrics.put("expectedTotal", expectedTotal);
        metrics.put("deliveredTotal", deliveredTotal);
        metrics.put("deliveryPercentage", activities.isEmpty() ? 0.0 : deliveryPercentageTotal / activities.size());
        metrics.put("generatedAt", OffsetDateTime.now(clock).toString());
        metrics.put("activities", activityMetrics);
        return metrics;
    }

    private Map<String, Object> activityMetric(ActivityEntity activity, LocalDate weekStartsAt, LocalDate weekEndsAt) {
        var progressRecords = progressRecords(activity, weekStartsAt, weekEndsAt);
        var delivered = progressRecords.stream()
                .mapToInt(record -> (int) record.get("value"))
                .sum();

        var metric = new LinkedHashMap<String, Object>();
        metric.put("activityId", activity.getId().toString());
        metric.put("description", activity.getDescription());
        metric.put("type", activity.getType().name());
        metric.put("goal", activity.getGoal());
        metric.put("delivered", delivered);
        metric.put("deliveryPercentage", percentage(delivered, activity.getGoal()));
        metric.put("weekStartsAt", activity.getWeekStartsAt().toString());
        metric.put("weekEndsAt", activity.getWeekEndsAt().toString());
        metric.put("progressRecords", progressRecords);
        return metric;
    }

    private List<Map<String, Object>> progressRecords(
            ActivityEntity activity, LocalDate weekStartsAt, LocalDate weekEndsAt) {
        if (activity.getType() == ActivityType.DAYS) {
            return dayRepository.findAllByActivityIdOrderByCreatedAtAsc(activity.getId()).stream()
                    .map(progress -> progressRecord(progress, 1, weekStartsAt, weekEndsAt))
                    .filter(record -> record != null)
                    .toList();
        }
        if (activity.getType() == ActivityType.COUNT) {
            return countRepository.findAllByActivityIdOrderByCreatedAtAsc(activity.getId()).stream()
                    .map(progress -> progressRecord(progress, progress.getValue(), weekStartsAt, weekEndsAt))
                    .filter(record -> record != null)
                    .toList();
        }
        return timeRepository.findAllByActivityIdOrderByCreatedAtAsc(activity.getId()).stream()
                .map(progress -> progressRecord(progress, progress.getMinutes(), weekStartsAt, weekEndsAt))
                .filter(record -> record != null)
                .toList();
    }

    private Map<String, Object> progressRecord(
            ActivityProgressDayEntity progress, int value, LocalDate weekStartsAt, LocalDate weekEndsAt) {
        return progressRecord(progress.getCreatedAt(), value, weekStartsAt, weekEndsAt);
    }

    private Map<String, Object> progressRecord(
            ActivityProgressCountEntity progress, int value, LocalDate weekStartsAt, LocalDate weekEndsAt) {
        return progressRecord(progress.getCreatedAt(), value, weekStartsAt, weekEndsAt);
    }

    private Map<String, Object> progressRecord(
            ActivityProgressTimeEntity progress, int value, LocalDate weekStartsAt, LocalDate weekEndsAt) {
        return progressRecord(progress.getCreatedAt(), value, weekStartsAt, weekEndsAt);
    }

    private Map<String, Object> progressRecord(
            OffsetDateTime registeredAt, int value, LocalDate weekStartsAt, LocalDate weekEndsAt) {
        var progressDate = registeredAt.toLocalDate();
        if (progressDate.isBefore(weekStartsAt)) {
            return null;
        }
        var record = new LinkedHashMap<String, Object>();
        record.put("registeredAt", registeredAt.toString());
        record.put("progressDate", progressDate.toString());
        record.put("value", value);
        record.put("daysRemainingToWeekEnd", Math.max(0, ChronoUnit.DAYS.between(progressDate, weekEndsAt)));
        return record;
    }

    private List<Map<String, Object>> previousReports(UUID calendarId, int week) {
        return reportRepository.findTop3ByCalendarIdAndWeekLessThanOrderByWeekDesc(calendarId, week).stream()
                .map(report -> {
                    Map<String, Object> previous = new LinkedHashMap<>();
                    previous.put("week", report.getWeek());
                    previous.put("weekStartsAt", report.getWeekStartsAt().toString());
                    previous.put("weekEndsAt", report.getWeekEndsAt().toString());
                    previous.put("metrics", report.getMetrics());
                    previous.put("markdownReport", report.getMarkdownReport());
                    return previous;
                })
                .toList();
    }

    private String buildPrompt(
            Map<String, Object> metrics, List<Map<String, Object>> previousReports, String userFeedback) {
        return """
                Voce e um avaliador de desempenho semanal da metodologia 12 Week Year.

                Escreva em portugues do Brasil, em Markdown, com no maximo 200 palavras.
                Use tom direto, pratico e respeitoso. Nao use motivacao generica, nao invente
                dados e nao crie novas metricas.

                Metricas da semana atual:
                {{current_week_metrics}}

                Relatorios das semanas anteriores, do mais recente para o mais antigo:
                {{previous_reports}}

                Feedback do usuario sobre a semana:
                {{user_feedback}}

                Regras da avaliacao:
                - Relacione o que foi entregue na semana atual com ate 3 semanas anteriores e
                  com o feedback do usuario.
                - Se nao houver historico, avalie somente a semana atual e o feedback.
                - Considere sucesso semanal somente quando deliveryPercentage for maior que
                  85%. O valor 85% exato nao deve ser classificado automaticamente como sucesso.
                - Nao liste todas as metricas nem detalhe cada atividade.
                - Produza exatamente estas duas secoes:

                # Avaliacao da Semana

                ## Recomendacao para a Proxima Semana
                """.replace("{{current_week_metrics}}", toJson(metrics))
                .replace("{{previous_reports}}", toJson(previousReports))
                .replace("{{user_feedback}}", userFeedback);
    }

    private double percentage(int delivered, int expected) {
        if (expected <= 0) {
            return 0.0;
        }
        return Math.min(100.0, (delivered * 100.0) / expected);
    }

    private LocalDate weekStartsAt(CalendarEntity calendar, int week) {
        return calendar.getStarts().plusDays((long) (week - 1) * 7);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize weekly report metrics");
        }
    }
}
