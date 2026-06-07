package br.com.ottonsam.toothy_planner_api.activity.dtos;

import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityType;
import java.util.UUID;

public record ActivityRequest(UUID calendarId, String description, Integer week, ActivityType type, String goal) {}
