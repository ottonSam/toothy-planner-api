package br.com.ottonsam.toothy_planner_api.activity.dtos;

import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityType;
import java.util.List;
import java.util.UUID;

public record ActivityCreationRequest(
        UUID calendarId,
        String description,
        Integer week,
        ActivityType type,
        String goal,
        List<Integer> replicateToWeeks) {

    public ActivityCreationRequest {
        replicateToWeeks = replicateToWeeks == null ? null : List.copyOf(replicateToWeeks);
    }

    @Override
    public List<Integer> replicateToWeeks() {
        return replicateToWeeks == null ? null : List.copyOf(replicateToWeeks);
    }
}
