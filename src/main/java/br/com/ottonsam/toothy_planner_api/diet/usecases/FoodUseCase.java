package br.com.ottonsam.toothy_planner_api.diet.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.diet.dtos.FoodResponse;
import br.com.ottonsam.toothy_planner_api.diet.entities.FoodEntity;
import br.com.ottonsam.toothy_planner_api.diet.repositories.FoodRepository;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FoodUseCase {

    private final FoodRepository foodRepository;
    private final FoodNutritionAiClient foodNutritionAiClient;
    private final CurrentUserProvider currentUserProvider;

    public FoodUseCase(
            FoodRepository foodRepository,
            FoodNutritionAiClient foodNutritionAiClient,
            CurrentUserProvider currentUserProvider) {
        this.foodRepository = foodRepository;
        this.foodNutritionAiClient = foodNutritionAiClient;
        this.currentUserProvider = currentUserProvider;
    }

    public List<FoodResponse> list(String name) {
        var user = currentUserProvider.get();
        var foods = name == null || name.isBlank()
                ? foodRepository.findAllByUserIdOrderByNameAsc(user.getId())
                : foodRepository.findAllByUserIdAndNameContainingOrderByNameAsc(
                        user.getId(), FoodNameNormalizer.normalize(name));
        return foods.stream().map(FoodResponse::from).toList();
    }

    public FoodResponse get(UUID id) {
        var user = currentUserProvider.get();
        return FoodResponse.from(findOwned(id, user.getId()));
    }

    FoodEntity findOrCreate(String foodName, UserEntity user) {
        var normalizedName = FoodNameNormalizer.normalize(foodName);
        return foodRepository
                .findByNameAndUserId(normalizedName, user.getId())
                .orElseGet(() -> createFromDeepSeek(normalizedName, user));
    }

    FoodEntity findOwned(UUID id, UUID userId) {
        if (id == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Food id is required");
        }
        return foodRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Food not found"));
    }

    private FoodEntity createFromDeepSeek(String normalizedName, UserEntity user) {
        var nutrition = foodNutritionAiClient.lookup(normalizedName);
        var food = FoodEntity.create(
                normalizedName,
                nutrition.perGram().kcal(),
                nutrition.perGram().protein(),
                nutrition.perGram().carbohydrate(),
                nutrition.perGram().fat(),
                nutrition.portion().kcal(),
                nutrition.portion().protein(),
                nutrition.portion().carbohydrate(),
                nutrition.portion().fat(),
                nutrition.portionDescription(),
                user);
        return foodRepository.save(food);
    }
}
