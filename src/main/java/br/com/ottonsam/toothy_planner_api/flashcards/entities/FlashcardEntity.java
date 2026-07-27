package br.com.ottonsam.toothy_planner_api.flashcards.entities;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "flashcards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlashcardEntity {

    @Id
    @NotNull(message = "Flashcard id is required") private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Flashcard user is required") private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    @NotNull(message = "Flashcard deck is required") private FlashcardDeckEntity deck;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Flashcard type is required") private FlashcardDeckType type;

    @Column
    private String word;

    @Column(name = "base_verb")
    private String baseVerb;

    @Column(name = "past_simple")
    private String pastSimple;

    @Column(name = "past_participle")
    private String pastParticiple;

    @Column
    private String expression;

    @Column(nullable = false)
    private String translation;

    @Column
    private String phonetic;

    @Column
    private String level;

    @Column(name = "usage_note", length = 1000)
    private String usageNote;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(name = "last_reviewed_at")
    private OffsetDateTime lastReviewedAt;

    @Column(name = "next_review_at")
    private OffsetDateTime nextReviewAt;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "wrong_count", nullable = false)
    private int wrongCount;

    @Column(name = "consecutive_correct", nullable = false)
    private int consecutiveCorrect;

    @Column(name = "consecutive_wrong", nullable = false)
    private int consecutiveWrong;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Flashcard difficulty is required") @DecimalMin(value = "0.0", message = "Flashcard difficulty must be greater than or equal to zero") private BigDecimal difficulty;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlashcardExampleEntity> examples = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "flashcard_card_tags",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<FlashcardTagEntity> tags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created at is required") private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull(message = "Updated at is required") private OffsetDateTime updatedAt;

    private FlashcardEntity(FlashcardDeckEntity deck, FlashcardDeckType type) {
        this.id = UUID.randomUUID();
        this.user = deck.getUser();
        this.deck = deck;
        this.type = type;
        this.active = true;
        this.difficulty = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = createdAt;
    }

    public static FlashcardEntity vocabulary(
            FlashcardDeckEntity deck,
            String word,
            String translation,
            String phonetic,
            String level,
            String usageNote,
            List<FlashcardExampleData> examples,
            Set<FlashcardTagEntity> tags) {
        validateDeck(deck);
        validateText(word, "Flashcard word is required");
        validateText(translation, "Flashcard translation is required");
        var card = new FlashcardEntity(deck, FlashcardDeckType.VOCABULARY);
        card.word = word.trim();
        card.translation = translation.trim();
        card.phonetic = trimToNull(phonetic);
        card.level = trimToNull(level);
        card.usageNote = trimToNull(usageNote);
        card.replaceExamples(examples);
        card.replaceTags(tags);
        return card;
    }

    public static FlashcardEntity irregularVerb(
            FlashcardDeckEntity deck,
            String baseVerb,
            String pastSimple,
            String pastParticiple,
            String translation,
            String phonetic,
            String usageNote,
            List<FlashcardExampleData> examples,
            Set<FlashcardTagEntity> tags) {
        validateDeck(deck);
        validateText(baseVerb, "Flashcard base verb is required");
        validateText(pastSimple, "Flashcard past simple is required");
        validateText(pastParticiple, "Flashcard past participle is required");
        validateText(translation, "Flashcard translation is required");
        var card = new FlashcardEntity(deck, FlashcardDeckType.IRREGULAR_VERBS);
        card.baseVerb = baseVerb.trim();
        card.pastSimple = pastSimple.trim();
        card.pastParticiple = pastParticiple.trim();
        card.translation = translation.trim();
        card.phonetic = trimToNull(phonetic);
        card.usageNote = trimToNull(usageNote);
        card.replaceExamples(examples);
        card.replaceTags(tags);
        return card;
    }

    public static FlashcardEntity expression(
            FlashcardDeckEntity deck,
            String expression,
            String translation,
            String phonetic,
            String usageNote,
            List<FlashcardExampleData> examples,
            Set<FlashcardTagEntity> tags) {
        validateDeck(deck);
        validateText(expression, "Flashcard expression is required");
        validateText(translation, "Flashcard translation is required");
        var card = new FlashcardEntity(deck, FlashcardDeckType.EXPRESSIONS);
        card.expression = expression.trim();
        card.translation = translation.trim();
        card.phonetic = trimToNull(phonetic);
        card.usageNote = trimToNull(usageNote);
        card.replaceExamples(examples);
        card.replaceTags(tags);
        return card;
    }

    public void updateVocabulary(
            String word,
            String translation,
            String phonetic,
            String level,
            String usageNote,
            List<FlashcardExampleData> examples,
            Set<FlashcardTagEntity> tags,
            boolean active) {
        requireType(FlashcardDeckType.VOCABULARY);
        validateText(word, "Flashcard word is required");
        validateText(translation, "Flashcard translation is required");
        this.word = word.trim();
        this.translation = translation.trim();
        this.phonetic = trimToNull(phonetic);
        this.level = trimToNull(level);
        this.usageNote = trimToNull(usageNote);
        this.active = active;
        replaceExamples(examples);
        replaceTags(tags);
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateIrregularVerb(
            String baseVerb,
            String pastSimple,
            String pastParticiple,
            String translation,
            String phonetic,
            String usageNote,
            List<FlashcardExampleData> examples,
            Set<FlashcardTagEntity> tags,
            boolean active) {
        requireType(FlashcardDeckType.IRREGULAR_VERBS);
        validateText(baseVerb, "Flashcard base verb is required");
        validateText(pastSimple, "Flashcard past simple is required");
        validateText(pastParticiple, "Flashcard past participle is required");
        validateText(translation, "Flashcard translation is required");
        this.baseVerb = baseVerb.trim();
        this.pastSimple = pastSimple.trim();
        this.pastParticiple = pastParticiple.trim();
        this.translation = translation.trim();
        this.phonetic = trimToNull(phonetic);
        this.usageNote = trimToNull(usageNote);
        this.active = active;
        replaceExamples(examples);
        replaceTags(tags);
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateExpression(
            String expression,
            String translation,
            String phonetic,
            String usageNote,
            List<FlashcardExampleData> examples,
            Set<FlashcardTagEntity> tags,
            boolean active) {
        requireType(FlashcardDeckType.EXPRESSIONS);
        validateText(expression, "Flashcard expression is required");
        validateText(translation, "Flashcard translation is required");
        this.expression = expression.trim();
        this.translation = translation.trim();
        this.phonetic = trimToNull(phonetic);
        this.usageNote = trimToNull(usageNote);
        this.active = active;
        replaceExamples(examples);
        replaceTags(tags);
        this.updatedAt = OffsetDateTime.now();
    }

    public void markSeen(OffsetDateTime seenAt) {
        if (seenAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard seen date is required");
        }
        this.lastSeenAt = seenAt;
        this.updatedAt = OffsetDateTime.now();
    }

    public void answer(FlashcardReviewRating rating, OffsetDateTime reviewedAt) {
        if (rating == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard review rating is required");
        }
        if (reviewedAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard review date is required");
        }
        this.lastReviewedAt = reviewedAt;
        this.reviewCount++;
        switch (rating) {
            case AGAIN -> applyWrong(reviewedAt, BigDecimal.valueOf(2), Duration.ofMinutes(10));
            case HARD -> applyCorrect(reviewedAt, BigDecimal.ONE, Duration.ofDays(1));
            case GOOD -> applyCorrect(reviewedAt, BigDecimal.valueOf(-0.5), Duration.ofDays(3));
            case EASY -> applyCorrect(reviewedAt, BigDecimal.valueOf(-1), Duration.ofDays(7));
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public String targetTerm() {
        return switch (type) {
            case VOCABULARY -> word;
            case IRREGULAR_VERBS -> baseVerb;
            case EXPRESSIONS -> expression;
        };
    }

    private void applyWrong(OffsetDateTime reviewedAt, BigDecimal difficultyDelta, Duration nextInterval) {
        this.wrongCount++;
        this.consecutiveWrong++;
        this.consecutiveCorrect = 0;
        updateDifficulty(difficultyDelta);
        this.nextReviewAt = reviewedAt.plus(nextInterval);
    }

    private void applyCorrect(OffsetDateTime reviewedAt, BigDecimal difficultyDelta, Duration nextInterval) {
        this.correctCount++;
        this.consecutiveCorrect++;
        this.consecutiveWrong = 0;
        updateDifficulty(difficultyDelta);
        this.nextReviewAt = reviewedAt.plus(nextInterval);
    }

    private void updateDifficulty(BigDecimal delta) {
        var next = difficulty.add(delta).setScale(2, RoundingMode.HALF_UP);
        if (next.signum() < 0) {
            next = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (next.compareTo(BigDecimal.TEN) > 0) {
            next = BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP);
        }
        this.difficulty = next;
    }

    private void replaceExamples(List<FlashcardExampleData> examples) {
        this.examples.clear();
        if (examples != null) {
            examples.forEach(example ->
                    this.examples.add(FlashcardExampleEntity.create(this, example.text(), example.translation())));
        }
    }

    private void replaceTags(Set<FlashcardTagEntity> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    private void requireType(FlashcardDeckType type) {
        if (this.type != type) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard type cannot be changed");
        }
    }

    private static void validateDeck(FlashcardDeckEntity deck) {
        if (deck == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard deck is required");
        }
    }

    private static void validateText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
