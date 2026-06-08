package br.com.ottonsam.toothy_planner_api.calendar.controllers;

import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityResponse;
import br.com.ottonsam.toothy_planner_api.activity.usecases.ActivityUseCase;
import br.com.ottonsam.toothy_planner_api.calendar.dtos.CalendarRequest;
import br.com.ottonsam.toothy_planner_api.calendar.dtos.CalendarResponse;
import br.com.ottonsam.toothy_planner_api.calendar.usecases.CalendarUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendars")
public class CalendarController {

    private final CalendarUseCase calendarUseCase;
    private final ActivityUseCase activityUseCase;

    public CalendarController(CalendarUseCase calendarUseCase, ActivityUseCase activityUseCase) {
        this.calendarUseCase = calendarUseCase;
        this.activityUseCase = activityUseCase;
    }

    @PostMapping
    ResponseEntity<CalendarResponse> create(@RequestBody CalendarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarUseCase.create(request));
    }

    @GetMapping
    List<CalendarResponse> list() {
        return calendarUseCase.list();
    }

    @GetMapping("/{id}")
    CalendarResponse get(@PathVariable UUID id) {
        return calendarUseCase.get(id);
    }

    @PutMapping("/{id}")
    CalendarResponse update(@PathVariable UUID id, @RequestBody CalendarRequest request) {
        return calendarUseCase.update(id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        calendarUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{calendarId}/weeks/{week}/activities")
    List<ActivityResponse> listActivitiesByWeek(@PathVariable UUID calendarId, @PathVariable int week) {
        return activityUseCase.listByCalendarAndWeek(calendarId, week);
    }
}
