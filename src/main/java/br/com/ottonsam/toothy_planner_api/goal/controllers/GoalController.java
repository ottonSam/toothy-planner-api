package br.com.ottonsam.toothy_planner_api.goal.controllers;

import br.com.ottonsam.toothy_planner_api.goal.dtos.GoalRequest;
import br.com.ottonsam.toothy_planner_api.goal.dtos.GoalResponse;
import br.com.ottonsam.toothy_planner_api.goal.dtos.OptionResponse;
import br.com.ottonsam.toothy_planner_api.goal.entities.GoalType;
import br.com.ottonsam.toothy_planner_api.goal.usecases.GoalUseCase;
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
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalUseCase goalUseCase;

    public GoalController(GoalUseCase goalUseCase) {
        this.goalUseCase = goalUseCase;
    }

    @PostMapping
    ResponseEntity<GoalResponse> create(@RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalUseCase.create(request));
    }

    @GetMapping
    List<GoalResponse> list() {
        return goalUseCase.list();
    }

    @GetMapping("/{id}")
    GoalResponse get(@PathVariable UUID id) {
        return goalUseCase.get(id);
    }

    @PutMapping("/{id}")
    GoalResponse update(@PathVariable UUID id, @RequestBody GoalRequest request) {
        return goalUseCase.update(id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        goalUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/types")
    List<OptionResponse> types() {
        return Arrays.stream(GoalType.values())
                .map(type -> new OptionResponse(type.getLabel(), type.name()))
                .toList();
    }
}
