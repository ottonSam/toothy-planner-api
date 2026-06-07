package br.com.ottonsam.toothy_planner_api.activity.dtos;

import java.util.UUID;

public record ActivityProgressTimeRequest(UUID activityId, String time) {}
