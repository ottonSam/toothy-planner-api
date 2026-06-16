package br.com.ottonsam.toothy_planner_api.diet.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "diet_foods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodEntity {

    @Id
    @NotNull(message = "Food id is required") private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Food name is required") private String name;

    @Column(name = "kcal_per_gram", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Food kcal per gram is required") @DecimalMin(value = "0.0", message = "Food kcal per gram must be greater than or equal to zero") private BigDecimal kcalPerGram;

    @Column(name = "protein_per_gram", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Food protein per gram is required") @DecimalMin(value = "0.0", message = "Food protein per gram must be greater than or equal to zero") private BigDecimal proteinPerGram;

    @Column(name = "carbohydrate_per_gram", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Food carbohydrate per gram is required") @DecimalMin(value = "0.0", message = "Food carbohydrate per gram must be greater than or equal to zero") private BigDecimal carbohydratePerGram;

    @Column(name = "fat_per_gram", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Food fat per gram is required") @DecimalMin(value = "0.0", message = "Food fat per gram must be greater than or equal to zero") private BigDecimal fatPerGram;

    @Column(name = "kcal_per_portion", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Food kcal per portion is required") @DecimalMin(value = "0.0", message = "Food kcal per portion must be greater than or equal to zero") private BigDecimal kcalPerPortion;

    @Column(name = "protein_per_portion", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Food protein per portion is required") @DecimalMin(value = "0.0", message = "Food protein per portion must be greater than or equal to zero") private BigDecimal proteinPerPortion;

    @Column(name = "carbohydrate_per_portion", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Food carbohydrate per portion is required") @DecimalMin(value = "0.0", message = "Food carbohydrate per portion must be greater than or equal to zero") private BigDecimal carbohydratePerPortion;

    @Column(name = "fat_per_portion", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Food fat per portion is required") @DecimalMin(value = "0.0", message = "Food fat per portion must be greater than or equal to zero") private BigDecimal fatPerPortion;

    @Column(name = "portion_description", nullable = false)
    @NotBlank(message = "Food portion description is required") private String portionDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Food user is required") private UserEntity user;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private FoodEntity(
            UUID id,
            String name,
            BigDecimal kcalPerGram,
            BigDecimal proteinPerGram,
            BigDecimal carbohydratePerGram,
            BigDecimal fatPerGram,
            BigDecimal kcalPerPortion,
            BigDecimal proteinPerPortion,
            BigDecimal carbohydratePerPortion,
            BigDecimal fatPerPortion,
            String portionDescription,
            UserEntity user) {
        this.id = id;
        this.name = name;
        this.kcalPerGram = kcalPerGram;
        this.proteinPerGram = proteinPerGram;
        this.carbohydratePerGram = carbohydratePerGram;
        this.fatPerGram = fatPerGram;
        this.kcalPerPortion = kcalPerPortion;
        this.proteinPerPortion = proteinPerPortion;
        this.carbohydratePerPortion = carbohydratePerPortion;
        this.fatPerPortion = fatPerPortion;
        this.portionDescription = portionDescription.trim();
        this.user = user;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static FoodEntity create(
            String name,
            BigDecimal kcalPerGram,
            BigDecimal proteinPerGram,
            BigDecimal carbohydratePerGram,
            BigDecimal fatPerGram,
            BigDecimal kcalPerPortion,
            BigDecimal proteinPerPortion,
            BigDecimal carbohydratePerPortion,
            BigDecimal fatPerPortion,
            String portionDescription,
            UserEntity user) {
        validateName(name);
        validateNutrition(kcalPerGram, "Food kcal per gram must be greater than or equal to zero");
        validateNutrition(proteinPerGram, "Food protein per gram must be greater than or equal to zero");
        validateNutrition(carbohydratePerGram, "Food carbohydrate per gram must be greater than or equal to zero");
        validateNutrition(fatPerGram, "Food fat per gram must be greater than or equal to zero");
        validateNutrition(kcalPerPortion, "Food kcal per portion must be greater than or equal to zero");
        validateNutrition(proteinPerPortion, "Food protein per portion must be greater than or equal to zero");
        validateNutrition(
                carbohydratePerPortion, "Food carbohydrate per portion must be greater than or equal to zero");
        validateNutrition(fatPerPortion, "Food fat per portion must be greater than or equal to zero");
        validatePortionDescription(portionDescription);
        validateUser(user);
        return new FoodEntity(
                UUID.randomUUID(),
                name.trim(),
                kcalPerGram,
                proteinPerGram,
                carbohydratePerGram,
                fatPerGram,
                kcalPerPortion,
                proteinPerPortion,
                carbohydratePerPortion,
                fatPerPortion,
                portionDescription,
                user);
    }

    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Food name is required");
        }
    }

    private static void validateNutrition(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private static void validatePortionDescription(String portionDescription) {
        if (portionDescription == null || portionDescription.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Food portion description is required");
        }
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Food user is required");
        }
    }
}
