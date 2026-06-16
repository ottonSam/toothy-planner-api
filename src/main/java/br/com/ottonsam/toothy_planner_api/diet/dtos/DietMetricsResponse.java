package br.com.ottonsam.toothy_planner_api.diet.dtos;

import java.time.LocalDate;
import java.util.List;

public record DietMetricsResponse(
        LocalDate date,
        DietGoalResponse goal,
        DietGoalResponse consumed,
        DietGoalResponse remaining,
        List<DietEntryResponse> entries) {

    public DietMetricsResponse {
        entries = List.copyOf(entries);
    }

    @Override
    public List<DietEntryResponse> entries() {
        return List.copyOf(entries);
    }
}
