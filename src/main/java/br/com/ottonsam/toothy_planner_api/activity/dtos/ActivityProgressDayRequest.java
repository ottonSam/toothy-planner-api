package br.com.ottonsam.toothy_planner_api.activity.dtos;

import br.com.ottonsam.toothy_planner_api.activity.entities.WeekDay;
import java.util.UUID;

public record ActivityProgressDayRequest(UUID activityId, WeekDay day) {}
