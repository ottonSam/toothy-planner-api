package br.com.ottonsam.toothy_planner_api.flashcards.entities;

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
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "flashcard_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashcardTagEntity {

    @Id
    @NotNull(message = "Flashcard tag id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Flashcard tag user is required") private UserEntity user;

    @Column(nullable = false)
    @NotBlank(message = "Flashcard tag name is required") private String name;

    @Column(name = "name_normalized", nullable = false)
    @NotBlank(message = "Flashcard tag normalized name is required") private String nameNormalized;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private FlashcardTagEntity(UUID id, UserEntity user, String name) {
        this.id = id;
        this.user = user;
        this.name = normalizeWhitespace(name);
        this.nameNormalized = normalizeValidatedName(name);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static FlashcardTagEntity create(UserEntity user, String name) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard tag user is required");
        }
        validateName(name);
        return new FlashcardTagEntity(UUID.randomUUID(), user, name);
    }

    public static String normalize(String name) {
        validateName(name);
        return normalizeValidatedName(name);
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard tag name is required");
        }
    }

    private static String normalizeValidatedName(String name) {
        return normalizeWhitespace(name).toLowerCase(Locale.ROOT);
    }

    private static String normalizeWhitespace(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
