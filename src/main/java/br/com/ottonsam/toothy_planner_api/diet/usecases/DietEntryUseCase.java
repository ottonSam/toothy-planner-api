package br.com.ottonsam.toothy_planner_api.diet.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietEntryRequest;
import br.com.ottonsam.toothy_planner_api.diet.dtos.DietEntryResponse;
import br.com.ottonsam.toothy_planner_api.diet.entities.DietEntryEntity;
import br.com.ottonsam.toothy_planner_api.diet.entities.DietEntryUnit;
import br.com.ottonsam.toothy_planner_api.diet.entities.FoodEntity;
import br.com.ottonsam.toothy_planner_api.diet.repositories.DietEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DietEntryUseCase {

    private final DietEntryRepository dietEntryRepository;
    private final FoodUseCase foodUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public DietEntryUseCase(
            DietEntryRepository dietEntryRepository,
            FoodUseCase foodUseCase,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.dietEntryRepository = dietEntryRepository;
        this.foodUseCase = foodUseCase;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public DietEntryResponse create(DietEntryRequest request) {
        var user = currentUserProvider.get();
        var food = foodUseCase.findOrCreate(request.foodName(), user);
        var entryDate = request.entryDate() == null ? LocalDate.now(clock) : request.entryDate();
        var quantity = requiredQuantity(request.quantity());
        var unit = requiredUnit(request.unit());
        var values = calculate(food, quantity, unit);
        var entry = DietEntryEntity.create(
                food,
                user,
                entryDate,
                quantity,
                unit,
                values.kcal(),
                values.protein(),
                values.carbohydrate(),
                values.fat());
        return DietEntryResponse.from(dietEntryRepository.save(entry));
    }

    public List<DietEntryResponse> list(LocalDate date) {
        var user = currentUserProvider.get();
        var entryDate = date == null ? LocalDate.now(clock) : date;
        return dietEntryRepository.findAllByUserIdAndEntryDateOrderByCreatedAtAsc(user.getId(), entryDate).stream()
                .map(DietEntryResponse::from)
                .toList();
    }

    public DietEntryResponse get(UUID entryId) {
        var user = currentUserProvider.get();
        return DietEntryResponse.from(findOwned(entryId, user.getId()));
    }

    public void delete(UUID entryId) {
        var user = currentUserProvider.get();
        dietEntryRepository.delete(findOwned(entryId, user.getId()));
    }

    DietEntryEntity findOwned(UUID entryId, UUID userId) {
        if (entryId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diet entry id is required");
        }
        return dietEntryRepository
                .findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Diet entry not found"));
    }

    private BigDecimal requiredQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diet entry quantity must be greater than zero");
        }
        return quantity;
    }

    private DietEntryUnit requiredUnit(DietEntryUnit unit) {
        if (unit == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diet entry unit is required");
        }
        return unit;
    }

    private NutritionValues calculate(FoodEntity food, BigDecimal quantity, DietEntryUnit unit) {
        if (unit == DietEntryUnit.GRAMS) {
            return new NutritionValues(
                    multiply(food.getKcalPerGram(), quantity),
                    multiply(food.getProteinPerGram(), quantity),
                    multiply(food.getCarbohydratePerGram(), quantity),
                    multiply(food.getFatPerGram(), quantity));
        }
        return new NutritionValues(
                multiply(food.getKcalPerPortion(), quantity),
                multiply(food.getProteinPerPortion(), quantity),
                multiply(food.getCarbohydratePerPortion(), quantity),
                multiply(food.getFatPerPortion(), quantity));
    }

    private BigDecimal multiply(BigDecimal value, BigDecimal quantity) {
        return value.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }
}
