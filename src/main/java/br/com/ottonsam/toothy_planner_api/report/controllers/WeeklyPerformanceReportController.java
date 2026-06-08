package br.com.ottonsam.toothy_planner_api.report.controllers;

import br.com.ottonsam.toothy_planner_api.report.dtos.WeeklyPerformanceReportRequest;
import br.com.ottonsam.toothy_planner_api.report.dtos.WeeklyPerformanceReportResponse;
import br.com.ottonsam.toothy_planner_api.report.usecases.WeeklyPerformanceReportUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendars")
public class WeeklyPerformanceReportController {

    private final WeeklyPerformanceReportUseCase useCase;

    public WeeklyPerformanceReportController(WeeklyPerformanceReportUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/{calendarId}/weeks/{week}/reports")
    ResponseEntity<WeeklyPerformanceReportResponse> create(
            @PathVariable UUID calendarId,
            @PathVariable int week,
            @RequestBody WeeklyPerformanceReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(calendarId, week, request));
    }

    @GetMapping("/{calendarId}/weeks/{week}/reports")
    WeeklyPerformanceReportResponse get(@PathVariable UUID calendarId, @PathVariable int week) {
        return useCase.get(calendarId, week);
    }

    @GetMapping("/{calendarId}/reports")
    List<WeeklyPerformanceReportResponse> list(@PathVariable UUID calendarId) {
        return useCase.list(calendarId);
    }
}
