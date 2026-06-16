package br.com.ottonsam.toothy_planner_api.diet.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietGoalRequest;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietGoalResponse;
import br.com.ottonsam.toothy_planner_api.diet.entities.DailyDietGoalEntity;
import br.com.ottonsam.toothy_planner_api.diet.entities.DietDefaultGoalEntity;
import br.com.ottonsam.toothy_planner_api.diet.repositories.DailyDietGoalRepository;
import br.com.ottonsam.toothy_planner_api.diet.repositories.DietDefaultGoalRepository;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DietGoalUseCase {

    private final DietDefaultGoalRepository defaultGoalRepository;
    private final DailyDietGoalRepository dailyGoalRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public DietGoalUseCase(
            DietDefaultGoalRepository defaultGoalRepository,
            DailyDietGoalRepository dailyGoalRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.defaultGoalRepository = defaultGoalRepository;
        this.dailyGoalRepository = dailyGoalRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public DietGoalResponse getDefaultGoal() {
        var user = currentUserProvider.get();
        return defaultGoalRepository
                .findByUserId(user.getId())
                .map(DietGoalResponse::from)
                .orElseGet(DietGoalResponse::zero);
    }

    public DietGoalResponse updateDefaultGoal(DietGoalRequest request) {
        var user = currentUserProvider.get();
        var defaultGoal = defaultGoalRepository
                .findByUserId(user.getId())
                .map(goal -> update(goal, request))
                .orElseGet(() -> createDefault(request, user));
        var today = LocalDate.now(clock);
        dailyGoalRepository.findAllByUserIdAndGoalDateGreaterThanEqual(user.getId(), today).stream()
                .forEach(goal -> goal.update(request.kcal(), request.protein(), request.carbohydrate(), request.fat()));
        dailyGoalRepository
                .findByUserIdAndGoalDate(user.getId(), today)
                .orElseGet(() -> dailyGoalRepository.save(DailyDietGoalEntity.create(
                        today, request.kcal(), request.protein(), request.carbohydrate(), request.fat(), user)));
        return DietGoalResponse.from(defaultGoal);
    }

    public DietGoalResponse getDailyGoal(LocalDate date) {
        var user = currentUserProvider.get();
        return resolveGoal(user.getId(), date == null ? LocalDate.now(clock) : date);
    }

    public DietGoalResponse updateDailyGoal(LocalDate date, DietGoalRequest request) {
        var user = currentUserProvider.get();
        var goalDate = date == null ? LocalDate.now(clock) : date;
        var dailyGoal = dailyGoalRepository
                .findByUserIdAndGoalDate(user.getId(), goalDate)
                .map(goal -> update(goal, request))
                .orElseGet(() -> dailyGoalRepository.save(DailyDietGoalEntity.create(
                        goalDate, request.kcal(), request.protein(), request.carbohydrate(), request.fat(), user)));
        return DietGoalResponse.from(dailyGoal);
    }

    DietGoalResponse resolveGoal(UUID userId, LocalDate date) {
        return dailyGoalRepository
                .findByUserIdAndGoalDate(userId, date)
                .map(DietGoalResponse::from)
                .or(() -> defaultGoalRepository.findByUserId(userId).map(DietGoalResponse::from))
                .orElseGet(DietGoalResponse::zero);
    }

    private DietDefaultGoalEntity createDefault(DietGoalRequest request, UserEntity user) {
        return defaultGoalRepository.save(DietDefaultGoalEntity.create(
                request.kcal(), request.protein(), request.carbohydrate(), request.fat(), user));
    }

    private DietDefaultGoalEntity update(DietDefaultGoalEntity goal, DietGoalRequest request) {
        goal.update(request.kcal(), request.protein(), request.carbohydrate(), request.fat());
        return defaultGoalRepository.save(goal);
    }

    private DailyDietGoalEntity update(DailyDietGoalEntity goal, DietGoalRequest request) {
        goal.update(request.kcal(), request.protein(), request.carbohydrate(), request.fat());
        return dailyGoalRepository.save(goal);
    }
}
