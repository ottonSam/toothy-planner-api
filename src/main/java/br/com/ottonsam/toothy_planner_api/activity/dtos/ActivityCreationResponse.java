package br.com.ottonsam.toothy_planner_api.activity.dtos;

import java.util.List;

public record ActivityCreationResponse(ActivityResponse activity, List<ActivityResponse> replicas) {

    public ActivityCreationResponse {
        replicas = List.copyOf(replicas);
    }

    @Override
    public List<ActivityResponse> replicas() {
        return List.copyOf(replicas);
    }
}
