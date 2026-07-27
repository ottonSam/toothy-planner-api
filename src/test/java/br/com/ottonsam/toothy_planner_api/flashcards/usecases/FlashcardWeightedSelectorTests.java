package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardEntity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlashcardWeightedSelectorTests {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 7, 27, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void selectsLowerWeightCandidateWhenDrawFallsInsideItsInterval() {
        var lowerWeightCard = mock(FlashcardEntity.class);
        var higherWeightCard = mock(FlashcardEntity.class);
        var selector = new FlashcardWeightedSelector(totalWeight -> {
            assertThat(totalWeight).isEqualTo(10.0);
            return 0.5;
        });

        var selected = selector.select(List.of(
                new WeightedFlashcardCandidate(lowerWeightCard, 1.0),
                new WeightedFlashcardCandidate(higherWeightCard, 9.0)));

        assertThat(selected.card()).isSameAs(lowerWeightCard);
        assertThat(selected.score()).isEqualTo(1.0);
    }

    @Test
    void selectsCandidateUsingCumulativeWeightIntervals() {
        var lowerWeightCard = mock(FlashcardEntity.class);
        var higherWeightCard = mock(FlashcardEntity.class);
        var selector = new FlashcardWeightedSelector(totalWeight -> 1.0);

        var selected = selector.select(List.of(
                new WeightedFlashcardCandidate(lowerWeightCard, 1.0),
                new WeightedFlashcardCandidate(higherWeightCard, 9.0)));

        assertThat(selected.card()).isSameAs(higherWeightCard);
        assertThat(selected.score()).isEqualTo(9.0);
    }

    @Test
    void givesNeverSeenCardMoreWeightThanRecentlySeenCard() {
        var useCase = reviewUseCase();
        var neverSeen = card(null, null);
        var recentlySeen = card(NOW.minusMinutes(1), null);

        assertThat(useCase.score(neverSeen, NOW)).isEqualTo(50.0);
        assertThat(useCase.score(recentlySeen, NOW)).isEqualTo(1.0);
    }

    @Test
    void increasesWeightForOverdueDifficultCardsWithErrors() {
        var useCase = reviewUseCase();
        var difficult = card(NOW.minusDays(5), NOW.minusHours(1));
        when(difficult.getWrongCount()).thenReturn(3);
        when(difficult.getConsecutiveWrong()).thenReturn(2);
        when(difficult.getDifficulty()).thenReturn(BigDecimal.valueOf(4));
        var easy = card(NOW.minusDays(5), null);
        when(easy.getConsecutiveCorrect()).thenReturn(4);

        assertThat(useCase.score(difficult, NOW)).isGreaterThan(useCase.score(easy, NOW));
    }

    @Test
    void keepsWeightAtLeastOneAfterRecentViewsAndConsecutiveCorrectAnswers() {
        var useCase = reviewUseCase();
        var card = card(NOW.minusMinutes(1), null);
        when(card.getConsecutiveCorrect()).thenReturn(100);

        assertThat(useCase.score(card, NOW)).isEqualTo(1.0);
    }

    private FlashcardReviewUseCase reviewUseCase() {
        return new FlashcardReviewUseCase(
                null,
                null,
                null,
                null,
                new FlashcardWeightedSelector(totalWeight -> 0.0),
                null,
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
    }

    private FlashcardEntity card(OffsetDateTime lastSeenAt, OffsetDateTime nextReviewAt) {
        var card = mock(FlashcardEntity.class);
        when(card.getLastSeenAt()).thenReturn(lastSeenAt);
        when(card.getNextReviewAt()).thenReturn(nextReviewAt);
        when(card.getDifficulty()).thenReturn(BigDecimal.ZERO);
        return card;
    }
}
