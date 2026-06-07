package br.com.ottonsam.toothy_planner_api.goal.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.goal.dtos.GoalRequest;
import br.com.ottonsam.toothy_planner_api.goal.dtos.GoalResponse;
import br.com.ottonsam.toothy_planner_api.goal.entities.GoalEntity;
import br.com.ottonsam.toothy_planner_api.goal.repositories.GoalRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoalUseCase {

    private final GoalRepository goalRepository;
    private final CurrentUserProvider currentUserProvider;

    public GoalUseCase(GoalRepository goalRepository, CurrentUserProvider currentUserProvider) {
        this.goalRepository = goalRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public GoalResponse create(GoalRequest request) {
        var user = currentUserProvider.get();
        var goal = GoalEntity.create(request.name(), request.type(), request.isComplete(), user);
        return GoalResponse.from(goalRepository.save(goal));
    }

    public List<GoalResponse> list() {
        var user = currentUserProvider.get();
        return goalRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(GoalResponse::from)
                .toList();
    }

    public GoalResponse get(UUID id) {
        var user = currentUserProvider.get();
        return GoalResponse.from(findOwned(id, user.getId()));
    }

    public GoalResponse update(UUID id, GoalRequest request) {
        var user = currentUserProvider.get();
        var goal = findOwned(id, user.getId());
        goal.update(request.name(), request.type(), request.isComplete());
        return GoalResponse.from(goalRepository.save(goal));
    }

    public void delete(UUID id) {
        var user = currentUserProvider.get();
        goalRepository.delete(findOwned(id, user.getId()));
    }

    private GoalEntity findOwned(UUID id, UUID userId) {
        return goalRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Goal not found"));
    }
}
