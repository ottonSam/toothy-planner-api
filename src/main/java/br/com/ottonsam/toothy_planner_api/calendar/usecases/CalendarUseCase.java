package br.com.ottonsam.toothy_planner_api.calendar.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.calendar.dtos.CalendarRequest;
import br.com.ottonsam.toothy_planner_api.calendar.dtos.CalendarResponse;
import br.com.ottonsam.toothy_planner_api.calendar.entities.CalendarEntity;
import br.com.ottonsam.toothy_planner_api.calendar.repositories.CalendarRepository;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.goal.entities.GoalEntity;
import br.com.ottonsam.toothy_planner_api.goal.repositories.GoalRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CalendarUseCase {

    private final CalendarRepository calendarRepository;
    private final GoalRepository goalRepository;
    private final CurrentUserProvider currentUserProvider;

    public CalendarUseCase(
            CalendarRepository calendarRepository,
            GoalRepository goalRepository,
            CurrentUserProvider currentUserProvider) {
        this.calendarRepository = calendarRepository;
        this.goalRepository = goalRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public CalendarResponse create(CalendarRequest request) {
        var user = currentUserProvider.get();
        var goals = findOwnedGoals(request.goalIds(), user.getId());
        var calendar = CalendarEntity.create(
                request.description(), requiredWeeks(request.weeks()), request.starts(), goals, user);
        return CalendarResponse.from(calendarRepository.save(calendar));
    }

    public List<CalendarResponse> list() {
        var user = currentUserProvider.get();
        return calendarRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(CalendarResponse::from)
                .toList();
    }

    public CalendarResponse get(UUID id) {
        var user = currentUserProvider.get();
        return CalendarResponse.from(findOwned(id, user.getId()));
    }

    public CalendarResponse update(UUID id, CalendarRequest request) {
        var user = currentUserProvider.get();
        var calendar = findOwned(id, user.getId());
        var goals = findOwnedGoals(request.goalIds(), user.getId());
        calendar.update(request.description(), requiredWeeks(request.weeks()), request.starts(), goals);
        return CalendarResponse.from(calendarRepository.save(calendar));
    }

    public void delete(UUID id) {
        var user = currentUserProvider.get();
        calendarRepository.delete(findOwned(id, user.getId()));
    }

    private CalendarEntity findOwned(UUID id, UUID userId) {
        return calendarRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Calendar not found"));
    }

    private Set<GoalEntity> findOwnedGoals(List<UUID> goalIds, UUID userId) {
        if (goalIds == null || goalIds.isEmpty()) {
            return Set.of();
        }
        var uniqueGoalIds = new LinkedHashSet<>(goalIds);
        var goals = goalRepository.findAllByIdInAndUserId(uniqueGoalIds, userId);
        if (goals.size() != uniqueGoalIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "All goals must belong to the authenticated user");
        }
        return Set.copyOf(goals);
    }

    private int requiredWeeks(Integer weeks) {
        if (weeks == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Calendar weeks is required");
        }
        return weeks;
    }
}
