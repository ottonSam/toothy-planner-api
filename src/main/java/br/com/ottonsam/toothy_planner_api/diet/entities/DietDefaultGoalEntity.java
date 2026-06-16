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
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "diet_default_goals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DietDefaultGoalEntity {

    @Id
    @NotNull(message = "Diet default goal id is required") private UUID id;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Diet goal kcal is required") @DecimalMin(value = "0.0", message = "Diet goal kcal must be greater than or equal to zero") private BigDecimal kcal;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Diet goal protein is required") @DecimalMin(value = "0.0", message = "Diet goal protein must be greater than or equal to zero") private BigDecimal protein;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Diet goal carbohydrate is required") @DecimalMin(value = "0.0", message = "Diet goal carbohydrate must be greater than or equal to zero") private BigDecimal carbohydrate;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Diet goal fat is required") @DecimalMin(value = "0.0", message = "Diet goal fat must be greater than or equal to zero") private BigDecimal fat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Diet default goal user is required") private UserEntity user;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private DietDefaultGoalEntity(
            UUID id, BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat, UserEntity user) {
        this.id = id;
        this.kcal = kcal;
        this.protein = protein;
        this.carbohydrate = carbohydrate;
        this.fat = fat;
        this.user = user;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static DietDefaultGoalEntity create(
            BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat, UserEntity user) {
        validate(kcal, protein, carbohydrate, fat);
        validateUser(user);
        return new DietDefaultGoalEntity(UUID.randomUUID(), kcal, protein, carbohydrate, fat, user);
    }

    public void update(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {
        validate(kcal, protein, carbohydrate, fat);
        this.kcal = kcal;
        this.protein = protein;
        this.carbohydrate = carbohydrate;
        this.fat = fat;
        this.updatedAt = OffsetDateTime.now();
    }

    public static void validate(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {
        validateGoalValue(kcal, "Diet goal kcal must be greater than or equal to zero");
        validateGoalValue(protein, "Diet goal protein must be greater than or equal to zero");
        validateGoalValue(carbohydrate, "Diet goal carbohydrate must be greater than or equal to zero");
        validateGoalValue(fat, "Diet goal fat must be greater than or equal to zero");
    }

    private static void validateGoalValue(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Diet default goal user is required");
        }
    }
}
