package br.com.ottonsam.toothy_planner_api.diet.controllers;

import br.com.ottonsam.toothy_planner_api.diet.dtos.DietEntryRequest;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietEntryResponse;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietGoalRequest;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietGoalResponse;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietMetricsResponse;
import br.com.ottonsam.toothy_planner_api.diet.dtos.FoodResponse;
import br.com.ottonsam.toothy_planner_api.diet.usecases.DietEntryUseCase;
import br.com.ottonsam.toothy_planner_api.diet.usecases.DietGoalUseCase;
import br.com.ottonsam.toothy_planner_api.diet.usecases.DietMetricsUseCase;
import br.com.ottonsam.toothy_planner_api.diet.usecases.FoodUseCase;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diet")
public class DietController {

    private final DietGoalUseCase dietGoalUseCase;
    private final FoodUseCase foodUseCase;
    private final DietEntryUseCase dietEntryUseCase;
    private final DietMetricsUseCase dietMetricsUseCase;

    public DietController(
            DietGoalUseCase dietGoalUseCase,
            FoodUseCase foodUseCase,
            DietEntryUseCase dietEntryUseCase,
            DietMetricsUseCase dietMetricsUseCase) {
        this.dietGoalUseCase = dietGoalUseCase;
        this.foodUseCase = foodUseCase;
        this.dietEntryUseCase = dietEntryUseCase;
        this.dietMetricsUseCase = dietMetricsUseCase;
    }

    @GetMapping("/goals/default")
    DietGoalResponse getDefaultGoal() {
        return dietGoalUseCase.getDefaultGoal();
    }

    @PutMapping("/goals/default")
    DietGoalResponse updateDefaultGoal(@RequestBody DietGoalRequest request) {
        return dietGoalUseCase.updateDefaultGoal(request);
    }

    @GetMapping("/goals/daily")
    DietGoalResponse getDailyGoal(@RequestParam(required = false) LocalDate date) {
        return dietGoalUseCase.getDailyGoal(date);
    }

    @PutMapping("/goals/daily")
    DietGoalResponse updateDailyGoal(
            @RequestParam(required = false) LocalDate date, @RequestBody DietGoalRequest request) {
        return dietGoalUseCase.updateDailyGoal(date, request);
    }

    @GetMapping("/foods")
    List<FoodResponse> listFoods(@RequestParam(required = false) String name) {
        return foodUseCase.list(name);
    }

    @GetMapping("/foods/{foodId}")
    FoodResponse getFood(@PathVariable UUID foodId) {
        return foodUseCase.get(foodId);
    }

    @PostMapping("/entries")
    ResponseEntity<DietEntryResponse> createEntry(@RequestBody DietEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dietEntryUseCase.create(request));
    }

    @GetMapping("/entries")
    List<DietEntryResponse> listEntries(@RequestParam(required = false) LocalDate date) {
        return dietEntryUseCase.list(date);
    }

    @GetMapping("/entries/{entryId}")
    DietEntryResponse getEntry(@PathVariable UUID entryId) {
        return dietEntryUseCase.get(entryId);
    }

    @DeleteMapping("/entries/{entryId}")
    ResponseEntity<Void> deleteEntry(@PathVariable UUID entryId) {
        dietEntryUseCase.delete(entryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/metrics")
    DietMetricsResponse metrics(@RequestParam(required = false) LocalDate date) {
        return dietMetricsUseCase.metrics(date);
    }
}
