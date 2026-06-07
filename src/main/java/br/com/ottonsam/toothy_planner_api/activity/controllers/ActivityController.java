package br.com.ottonsam.toothy_planner_api.activity.controllers;

import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityProgressCountRequest;
import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityProgressDayRequest;
import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityProgressTimeRequest;
import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityRequest;
import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityResponse;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityType;
import br.com.ottonsam.toothy_planner_api.activity.entities.WeekDay;
import br.com.ottonsam.toothy_planner_api.activity.usecases.ActivityProgressUseCase;
import br.com.ottonsam.toothy_planner_api.activity.usecases.ActivityUseCase;
import br.com.ottonsam.toothy_planner_api.goal.dtos.OptionResponse;
import java.util.Arrays;
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
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityUseCase activityUseCase;
    private final ActivityProgressUseCase activityProgressUseCase;

    public ActivityController(ActivityUseCase activityUseCase, ActivityProgressUseCase activityProgressUseCase) {
        this.activityUseCase = activityUseCase;
        this.activityProgressUseCase = activityProgressUseCase;
    }

    @PostMapping
    ResponseEntity<ActivityResponse> create(@RequestBody ActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityUseCase.create(request));
    }

    @GetMapping
    List<ActivityResponse> list() {
        return activityUseCase.list();
    }

    @GetMapping("/{id}")
    ActivityResponse get(@PathVariable UUID id) {
        return activityUseCase.get(id);
    }

    @PutMapping("/{id}")
    ActivityResponse update(@PathVariable UUID id, @RequestBody ActivityRequest request) {
        return activityUseCase.update(id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        activityUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/types")
    List<OptionResponse> types() {
        return Arrays.stream(ActivityType.values())
                .map(type -> new OptionResponse(type.getLabel(), type.name()))
                .toList();
    }

    @GetMapping("/days")
    List<OptionResponse> days() {
        return Arrays.stream(WeekDay.values())
                .map(day -> new OptionResponse(day.getLabel(), day.name()))
                .toList();
    }

    @PostMapping("/progress/days")
    ActivityResponse registerDay(@RequestBody ActivityProgressDayRequest request) {
        return activityProgressUseCase.registerDay(request);
    }

    @PostMapping("/progress/count")
    ActivityResponse registerCount(@RequestBody ActivityProgressCountRequest request) {
        return activityProgressUseCase.registerCount(request);
    }

    @PostMapping("/progress/time")
    ActivityResponse registerTime(@RequestBody ActivityProgressTimeRequest request) {
        return activityProgressUseCase.registerTime(request);
    }
}
