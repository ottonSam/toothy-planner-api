package br.com.ottonsam.toothy_planner_api.diet.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietEntryResponse;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietGoalResponse;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietMetricsResponse;
import br.com.ottonsam.toothy_planner_api.diet.repositories.DietEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DietMetricsUseCase {

    private final DietEntryRepository dietEntryRepository;
    private final DietGoalUseCase dietGoalUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public DietMetricsUseCase(
            DietEntryRepository dietEntryRepository,
            DietGoalUseCase dietGoalUseCase,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.dietEntryRepository = dietEntryRepository;
        this.dietGoalUseCase = dietGoalUseCase;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public DietMetricsResponse metrics(LocalDate date) {
        var user = currentUserProvider.get();
        var metricsDate = date == null ? LocalDate.now(clock) : date;
        var entries = dietEntryRepository.findAllByUserIdAndEntryDateOrderByCreatedAtAsc(user.getId(), metricsDate);
        var consumed = new DietGoalResponse(
                sum(entries.stream().map(entry -> entry.getKcal())),
                sum(entries.stream().map(entry -> entry.getProtein())),
                sum(entries.stream().map(entry -> entry.getCarbohydrate())),
                sum(entries.stream().map(entry -> entry.getFat())));
        var goal = dietGoalUseCase.resolveGoal(user.getId(), metricsDate);
        var remaining = new DietGoalResponse(
                subtract(goal.kcal(), consumed.kcal()),
                subtract(goal.protein(), consumed.protein()),
                subtract(goal.carbohydrate(), consumed.carbohydrate()),
                subtract(goal.fat(), consumed.fat()));
        return new DietMetricsResponse(
                metricsDate,
                goal,
                consumed,
                remaining,
                entries.stream().map(DietEntryResponse::from).toList());
    }

    private BigDecimal sum(java.util.stream.Stream<BigDecimal> values) {
        return values.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return left.subtract(right).setScale(2, RoundingMode.HALF_UP);
    }
}
