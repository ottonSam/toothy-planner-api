package br.com.ottonsam.toothy_planner_api.calendar.dtos;

import br.com.ottonsam.toothy_planner_api.calendar.entities.CalendarEntity;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record CalendarResponse(UUID id, String description, int weeks, LocalDate starts, List<UUID> goalIds) {

    public CalendarResponse {
        goalIds = List.copyOf(goalIds);
    }

    @Override
    public List<UUID> goalIds() {
        return List.copyOf(goalIds);
    }

    public static CalendarResponse from(CalendarEntity calendar) {
        var goalIds = calendar.getGoals().stream()
                .map(goal -> goal.getId())
                .sorted(Comparator.naturalOrder())
                .toList();
        return new CalendarResponse(
                calendar.getId(), calendar.getDescription(), calendar.getWeeks(), calendar.getStarts(), goalIds);
    }
}
