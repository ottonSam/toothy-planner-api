package br.com.ottonsam.toothy_planner_api.flashcards.entities;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "flashcard_decks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashcardDeckEntity {

    @Id
    @NotNull(message = "Flashcard deck id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Flashcard deck user is required") private UserEntity user;

    @Column(nullable = false)
    @NotBlank(message = "Flashcard deck name is required") private String name;

    @Column(nullable = false)
    @NotBlank(message = "Flashcard deck context is required") private String context;

    @Column(name = "target_language", nullable = false)
    @NotBlank(message = "Flashcard deck target language is required") private String targetLanguage;

    @Column(name = "base_language", nullable = false)
    @NotBlank(message = "Flashcard deck base language is required") private String baseLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Flashcard deck type is required") private FlashcardDeckType type;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private FlashcardDeckEntity(
            UUID id,
            UserEntity user,
            String name,
            String context,
            String targetLanguage,
            String baseLanguage,
            FlashcardDeckType type) {
        this.id = id;
        this.user = user;
        this.name = name.trim();
        this.context = context.trim();
        this.targetLanguage = targetLanguage.trim();
        this.baseLanguage = baseLanguage.trim();
        this.type = type;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static FlashcardDeckEntity create(
            UserEntity user,
            String name,
            String context,
            String targetLanguage,
            String baseLanguage,
            FlashcardDeckType type) {
        validateUser(user);
        validateName(name);
        validateContext(context);
        validateTargetLanguage(targetLanguage);
        validateBaseLanguage(baseLanguage);
        validateType(type);
        return new FlashcardDeckEntity(UUID.randomUUID(), user, name, context, targetLanguage, baseLanguage, type);
    }

    public void update(
            String name, String context, String targetLanguage, String baseLanguage, FlashcardDeckType type) {
        validateName(name);
        validateContext(context);
        validateTargetLanguage(targetLanguage);
        validateBaseLanguage(baseLanguage);
        validateType(type);
        this.name = name.trim();
        this.context = context.trim();
        this.targetLanguage = targetLanguage.trim();
        this.baseLanguage = baseLanguage.trim();
        this.type = type;
        this.updatedAt = OffsetDateTime.now();
    }

    private static void validateUser(UserEntity user) {
        if (user == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard deck user is required");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard deck name is required");
        }
    }

    private static void validateContext(String context) {
        if (context == null || context.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard deck context is required");
        }
    }

    private static void validateTargetLanguage(String targetLanguage) {
        if (targetLanguage == null || targetLanguage.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard deck target language is required");
        }
    }

    private static void validateBaseLanguage(String baseLanguage) {
        if (baseLanguage == null || baseLanguage.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard deck base language is required");
        }
    }

    private static void validateType(FlashcardDeckType type) {
        if (type == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard deck type is required");
        }
    }
}
