package br.com.ottonsam.toothy_planner_api.calendar.dtos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CalendarRequest(String description, Integer weeks, LocalDate starts, List<UUID> goalIds) {

    public CalendarRequest {
        goalIds = goalIds == null ? null : List.copyOf(goalIds);
    }

    @Override
    public List<UUID> goalIds() {
        return goalIds == null ? null : List.copyOf(goalIds);
    }
}
