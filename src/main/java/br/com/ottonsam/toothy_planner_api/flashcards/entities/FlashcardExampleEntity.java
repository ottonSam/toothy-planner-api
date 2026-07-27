package br.com.ottonsam.toothy_planner_api.flashcards.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "flashcard_examples")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashcardExampleEntity {

    @Id
    @NotNull(message = "Flashcard example id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    @NotNull(message = "Flashcard example card is required") private FlashcardEntity card;

    @Column(nullable = false, length = 1000)
    @NotBlank(message = "Flashcard example text is required") private String text;

    @Column(nullable = false, length = 1000)
    @NotBlank(message = "Flashcard example translation is required") private String translation;

    private FlashcardExampleEntity(UUID id, FlashcardEntity card, String text, String translation) {
        this.id = id;
        this.card = card;
        this.text = text.trim();
        this.translation = translation.trim();
    }

    public static FlashcardExampleEntity create(FlashcardEntity card, String text, String translation) {
        if (card == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard example card is required");
        }
        validateText(text);
        validateTranslation(translation);
        return new FlashcardExampleEntity(UUID.randomUUID(), card, text, translation);
    }

    public void update(String text, String translation) {
        validateText(text);
        validateTranslation(translation);
        this.text = text.trim();
        this.translation = translation.trim();
    }

    private static void validateText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard example text is required");
        }
    }

    private static void validateTranslation(String translation) {
        if (translation == null || translation.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard example translation is required");
        }
    }
}
