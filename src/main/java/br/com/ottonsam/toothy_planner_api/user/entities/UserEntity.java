package br.com.ottonsam.toothy_planner_api.user.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final java.util.regex.Pattern STRONG_PASSWORD_PATTERN =
            java.util.regex.Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    @Id
    @NotNull(message = "User id is required") private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Name is required") @Size(min = 2, message = "Name must have at least 2 characters") private String name;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required") @Email(message = "Email must be valid") private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required") private String password;

    @Column(name = "profile_image")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Theme is required") private UserTheme theme;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private UserEntity(
            UUID id, String name, String email, String encodedPassword, String profileImage, UserTheme theme) {
        this.id = id;
        this.name = name.trim();
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = encodedPassword;
        this.profileImage = profileImage;
        this.theme = theme == null ? UserTheme.LIGHT : theme;
        this.active = false;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static UserEntity create(
            String name,
            String email,
            String rawPassword,
            String encodedPassword,
            String profileImage,
            UserTheme theme) {
        validateName(name);
        validateEmail(email);
        validateStrongPassword(rawPassword);
        return new UserEntity(UUID.randomUUID(), name, email, encodedPassword, profileImage, theme);
    }

    public void updateProfile(String name, String rawPassword, String encodedPassword, UserTheme theme) {
        if (name != null) {
            validateName(name);
            this.name = name.trim();
        }
        if (rawPassword != null) {
            validateStrongPassword(rawPassword);
            this.password = encodedPassword;
        }
        if (theme != null) {
            this.theme = theme;
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
        this.updatedAt = OffsetDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public static void validateName(String name) {
        if (name == null || name.trim().length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name must have at least 2 characters");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email must be valid");
        }
    }

    public static void validateStrongPassword(String password) {
        if (password == null || !STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Password must have at least 8 characters, one uppercase letter, one lowercase letter, one number and one special character");
        }
    }

    public static String normalizeEmail(String email) {
        validateEmail(email);
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isActive() {
        return active;
    }
}
