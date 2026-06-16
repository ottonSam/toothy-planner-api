package br.com.ottonsam.toothy_planner_api.financial_manager.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "expense_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseCategoryEntity {

    @Id
    @NotNull(message = "Category id is required") private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Category name is required") private String name;

    @Column(nullable = false)
    @NotBlank(message = "Category color is required") private String color;

    @Column(nullable = false)
    @NotBlank(message = "Category icon is required") private String icon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Category user is required") private UserEntity user;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private ExpenseCategoryEntity(UUID id, String name, String color, String icon, UserEntity user) {
        this.id = id;
        this.name = name.trim();
        this.color = color.trim();
        this.icon = icon.trim();
        this.user = user;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static ExpenseCategoryEntity create(String name, String color, String icon, UserEntity user) {
        validateName(name);
        validateColor(color);
        validateIcon(icon);
        validateUser(user);
        return new ExpenseCategoryEntity(UUID.randomUUID(), name, color, icon, user);
    }

    public void update(String name, String color, String icon) {
        validateName(name);
        validateColor(color);
        validateIcon(icon);
        this.name = name.trim();
        this.color = color.trim();
        this.icon = icon.trim();
        this.updatedAt = OffsetDateTime.now();
    }

    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category name is required");
        }
    }

    public static void validateColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category color is required");
        }
    }

    public static void validateIcon(String icon) {
        if (icon == null || icon.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category icon is required");
        }
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category user is required");
        }
    }
}
