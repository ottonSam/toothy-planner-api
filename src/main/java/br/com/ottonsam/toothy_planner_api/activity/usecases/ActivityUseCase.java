package br.com.ottonsam.toothy_planner_api.activity.usecases;

import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityRequest;
import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityResponse;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityType;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityRepository;
import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.calendar.entities.CalendarEntity;
import br.com.ottonsam.toothy_planner_api.calendar.repositories.CalendarRepository;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActivityUseCase {

    private final ActivityRepository activityRepository;
    private final CalendarRepository calendarRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TimeTextParser timeTextParser;
    private final ActivityProgressReader activityProgressReader;

    public ActivityUseCase(
            ActivityRepository activityRepository,
            CalendarRepository calendarRepository,
            CurrentUserProvider currentUserProvider,
            TimeTextParser timeTextParser,
            ActivityProgressReader activityProgressReader) {
        this.activityRepository = activityRepository;
        this.calendarRepository = calendarRepository;
        this.currentUserProvider = currentUserProvider;
        this.timeTextParser = timeTextParser;
        this.activityProgressReader = activityProgressReader;
    }

    public ActivityResponse create(ActivityRequest request) {
        var user = currentUserProvider.get();
        var calendar = findOwnedCalendar(request.calendarId(), user.getId());
        var goal = parseGoal(request.type(), request.goal());
        var activity = ActivityEntity.create(
                request.description(), requiredWeek(request.week()), calendar, request.type(), goal);
        return activityProgressReader.toResponse(activityRepository.save(activity));
    }

    public List<ActivityResponse> list() {
        var user = currentUserProvider.get();
        return activityRepository.findAllByCalendarUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(activityProgressReader::toResponse)
                .toList();
    }

    public ActivityResponse get(UUID id) {
        var user = currentUserProvider.get();
        return activityProgressReader.toResponse(findOwned(id, user.getId()));
    }

    public ActivityResponse update(UUID id, ActivityRequest request) {
        var user = currentUserProvider.get();
        var activity = findOwned(id, user.getId());
        var calendar = findOwnedCalendar(request.calendarId(), user.getId());
        var goal = parseGoal(request.type(), request.goal());
        activity.update(request.description(), requiredWeek(request.week()), calendar, request.type(), goal);
        return activityProgressReader.toResponse(activityRepository.save(activity));
    }

    public void delete(UUID id) {
        var user = currentUserProvider.get();
        activityRepository.delete(findOwned(id, user.getId()));
    }

    ActivityEntity findOwned(UUID id, UUID userId) {
        return activityRepository
                .findByIdAndCalendarUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Activity not found"));
    }

    private CalendarEntity findOwnedCalendar(UUID calendarId, UUID userId) {
        if (calendarId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Calendar id is required");
        }
        return calendarRepository
                .findByIdAndUserId(calendarId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Calendar not found"));
    }

    private int requiredWeek(Integer week) {
        if (week == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activity week is required");
        }
        return week;
    }

    private int parseGoal(ActivityType type, String goal) {
        if (type == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activity type is required");
        }
        if (type == ActivityType.TIME) {
            return timeTextParser.parse(goal);
        }
        if (goal == null || goal.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activity goal is required");
        }
        try {
            var parsedGoal = Integer.parseInt(goal.trim());
            ActivityEntity.validateGoal(parsedGoal);
            return parsedGoal;
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Activity goal must be an integer");
        }
    }
}
