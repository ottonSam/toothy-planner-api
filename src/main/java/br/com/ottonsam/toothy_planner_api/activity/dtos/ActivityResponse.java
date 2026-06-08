package br.com.ottonsam.toothy_planner_api.activity.dtos;

import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityType;
import br.com.ottonsam.toothy_planner_api.activity.entities.WeekDay;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID calendarId,
        String description,
        int week,
        ActivityType type,
        int goal,
        LocalDate weekStartsAt,
        LocalDate weekEndsAt,
        int progress,
        List<WeekDay> progressDays) {

    public ActivityResponse {
        progressDays = List.copyOf(progressDays);
    }

    @Override
    public List<WeekDay> progressDays() {
        return List.copyOf(progressDays);
    }

    public static ActivityResponse from(ActivityEntity activity, int progress, List<WeekDay> progressDays) {
        return new ActivityResponse(
                activity.getId(),
                activity.getCalendar().getId(),
                activity.getDescription(),
                activity.getWeek(),
                activity.getType(),
                activity.getGoal(),
                activity.getWeekStartsAt(),
                activity.getWeekEndsAt(),
                progress,
                List.copyOf(progressDays));
    }
}
