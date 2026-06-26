package br.com.ottonsam.toothy_planner_api.diet.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "diet_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DietEntryEntity {

    @Id
    @NotNull(message = "Diet entry id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    @NotNull(message = "Diet entry food is required") private FoodEntity food;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Diet entry user is required") private UserEntity user;

    @Column(name = "entry_date", nullable = false)
    @NotNull(message = "Diet entry date is required") private LocalDate entryDate;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Diet entry quantity is required") @DecimalMin(value = "0.0001", message = "Diet entry quantity must be greater than zero") private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Diet entry unit is required") private DietEntryUnit unit;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Diet entry kcal is required") private BigDecimal kcal;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Diet entry protein is required") private BigDecimal protein;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Diet entry carbohydrate is required") private BigDecimal carbohydrate;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Diet entry fat is required") private BigDecimal fat;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private DietEntryEntity(
            UUID id,
            FoodEntity food,
            UserEntity user,
            LocalDate entryDate,
            BigDecimal quantity,
            DietEntryUnit unit,
            BigDecimal kcal,
            BigDecimal protein,
            BigDecimal carbohydrate,
            BigDecimal fat) {
        this.id = id;
        this.food = food;
        this.user = user;
        this.entryDate = entryDate;
        this.quantity = quantity;
        this.unit = unit;
        this.kcal = kcal;
        this.protein = protein;
        this.carbohydrate = carbohydrate;
        this.fat = fat;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static DietEntryEntity create(
            FoodEntity food,
            UserEntity user,
            LocalDate entryDate,
            BigDecimal quantity,
            DietEntryUnit unit,
            BigDecimal kcal,
            BigDecimal protein,
            BigDecimal carbohydrate,
            BigDecimal fat) {
        validateFood(food);
        validateUser(user);
        validateEntryDate(entryDate);
        validateQuantity(quantity);
        validateUnit(unit);
        validateCalculated(kcal, "Diet entry kcal is required");
        validateCalculated(protein, "Diet entry protein is required");
        validateCalculated(carbohydrate, "Diet entry carbohydrate is required");
        validateCalculated(fat, "Diet entry fat is required");
        return new DietEntryEntity(
                UUID.randomUUID(), food, user, entryDate, quantity, unit, kcal, protein, carbohydrate, fat);
    }

    public void updateQuantityAndUnit(
            BigDecimal quantity,
            DietEntryUnit unit,
            BigDecimal kcal,
            BigDecimal protein,
            BigDecimal carbohydrate,
            BigDecimal fat) {
        validateQuantity(quantity);
        validateUnit(unit);
        validateCalculated(kcal, "Diet entry kcal is required");
        validateCalculated(protein, "Diet entry protein is required");
        validateCalculated(carbohydrate, "Diet entry carbohydrate is required");
        validateCalculated(fat, "Diet entry fat is required");
        this.quantity = quantity;
        this.unit = unit;
        this.kcal = kcal;
        this.protein = protein;
        this.carbohydrate = carbohydrate;
        this.fat = fat;
        this.updatedAt = OffsetDateTime.now();
    }

    private static void validateFood(FoodEntity food) {
        if (food == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diet entry food is required");
        }
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diet entry user is required");
        }
    }

    private static void validateEntryDate(LocalDate entryDate) {
        if (entryDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diet entry date is required");
        }
    }

    private static void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diet entry quantity must be greater than zero");
        }
    }

    private static void validateUnit(DietEntryUnit unit) {
        if (unit == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diet entry unit is required");
        }
    }

    private static void validateCalculated(BigDecimal value, String message) {
        if (value == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
