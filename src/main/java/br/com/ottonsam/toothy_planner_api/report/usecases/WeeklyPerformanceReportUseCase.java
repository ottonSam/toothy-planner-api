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
        for (var activity : activities) {
            var metric = activityMetric(activity, weekStartsAt, weekEndsAt);
            activityMetrics.add(metric);
            expectedTotal += activity.getGoal();
            deliveredTotal += (int) metric.get("delivered");
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
        metrics.put("deliveryPercentage", percentage(deliveredTotal, expectedTotal));
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
                Voce e um coach de desempenho especializado na metodologia 12 Week Year.

                Gere um relatorio semanal de desempenho em Markdown, inteiramente em portugues
                do Brasil, para um usuario que esta executando um calendario de metas de 12
                semanas.

                Principios que devem guiar a analise:
                - Foque em execucao, nao em motivacao generica.
                - Compare compromissos planejados com entregas reais.
                - Identifique padroes de consistencia, timing e cumprimento.
                - Trate a execucao semanal como principal indicador de alcance das metas.
                - Recomende ajustes concretos para a proxima semana.
                - Use tom direto, pratico e respeitoso.
                - Nao invente dados que nao foram fornecidos.
                - Nao crie metricas novas alem das informadas.
                - Quando houver progresso acima da meta, reconheca o excedente, mas considere a
                  metrica percentual limitada a 100%.

                Contexto do calendario:
                {{calendar_context}}

                Metricas da semana atual:
                {{current_week_metrics}}

                Metricas das atividades:
                {{activities_metrics}}

                Registros de progresso:
                {{progress_records}}

                Relatorios das semanas anteriores, do mais recente para o mais antigo:
                {{previous_reports}}

                Feedback do usuario sobre a semana:
                {{user_feedback}}

                Gere o relatorio em Markdown com exatamente estas secoes:

                # Relatorio Semanal de Desempenho

                ## Resumo
                Resuma brevemente como a semana performou em relacao ao plano.

                ## Metricas de Execucao
                Liste as principais metricas de entrega, incluindo total esperado, total
                entregue, percentual de entrega da semana e destaques relevantes por atividade.

                ## Analise de Timing
                Analise quando o progresso foi registrado, quantos dias restavam para o fim da
                semana e se a execucao foi antecipada, tardia, concentrada ou consistente.

                ## Padroes Das Semanas Anteriores
                Compare a semana atual com ate 3 relatorios anteriores e identifique tendencias.
                Se nao houver relatorios anteriores, informe que ainda nao ha historico
                suficiente.

                ## Feedback Do Usuario
                Conecte o sentimento e o feedback textual do usuario com os dados de execucao.

                ## Recomendacoes Para A Proxima Semana
                Forneca recomendacoes praticas para a proxima semana, alinhadas ao 12 Week Year.

                ## Foco De Compromisso
                Finalize com 3 compromissos ou ajustes claros para a proxima semana.
                """.replace(
                        "{{calendar_context}}",
                        toJson(Map.of(
                                "calendarId",
                                metrics.get("calendarId"),
                                "calendarDescription",
                                metrics.get("calendarDescription"),
                                "weekStartsOn",
                                metrics.get("weekStartsOn"),
                                "weekEndsOn",
                                metrics.get("weekEndsOn"))))
                .replace("{{current_week_metrics}}", toJson(metrics))
                .replace("{{activities_metrics}}", toJson(metrics.get("activities")))
                .replace("{{progress_records}}", toJson(progressRecordsFromMetrics(metrics)))
                .replace("{{previous_reports}}", toJson(previousReports))
                .replace("{{user_feedback}}", userFeedback);
    }

    @SuppressWarnings("unchecked")
    private List<Object> progressRecordsFromMetrics(Map<String, Object> metrics) {
        var records = new ArrayList<>();
        for (var activity : (List<Map<String, Object>>) metrics.get("activities")) {
            records.add(Map.of(
                    "activityId",
                    activity.get("activityId"),
                    "description",
                    activity.get("description"),
                    "progressRecords",
                    activity.get("progressRecords")));
        }
        return records;
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
