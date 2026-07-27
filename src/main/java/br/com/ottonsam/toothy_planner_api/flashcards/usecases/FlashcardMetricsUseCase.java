package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardMetricsResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardReviewRating;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardDeckRepository;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardRepository;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardReviewAnswerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FlashcardMetricsUseCase {

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardRepository cardRepository;
    private final FlashcardReviewAnswerRepository answerRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public FlashcardMetricsUseCase(
            FlashcardDeckRepository deckRepository,
            FlashcardRepository cardRepository,
            FlashcardReviewAnswerRepository answerRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.answerRepository = answerRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public FlashcardMetricsResponse metrics() {
        var user = currentUserProvider.get();
        var now = OffsetDateTime.now(clock);
        var startOfDay = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        var startOfWeek = now.minusDays(7);
        var totalCorrect = answerRepository.countByUserIdAndRating(user.getId(), FlashcardReviewRating.HARD)
                + answerRepository.countByUserIdAndRating(user.getId(), FlashcardReviewRating.GOOD)
                + answerRepository.countByUserIdAndRating(user.getId(), FlashcardReviewRating.EASY);
        var totalWrong = answerRepository.countByUserIdAndRating(user.getId(), FlashcardReviewRating.AGAIN);
        var totalAnswers = totalCorrect + totalWrong;
        var accuracyRate = totalAnswers == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(totalCorrect)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalAnswers), 2, RoundingMode.HALF_UP);
        var dueCards = cardRepository.findAllByUserIdAndActiveTrue(user.getId()).stream()
                .filter(card -> card.getNextReviewAt() != null
                        && !card.getNextReviewAt().isAfter(now))
                .count();
        return new FlashcardMetricsResponse(
                deckRepository.countByUserId(user.getId()),
                cardRepository.countByUserIdAndActiveTrue(user.getId()),
                answerRepository.countByUserIdAndAnsweredAtGreaterThanEqual(user.getId(), startOfDay),
                answerRepository.countByUserIdAndAnsweredAtGreaterThanEqual(user.getId(), startOfWeek),
                totalCorrect,
                totalWrong,
                accuracyRate,
                dueCards,
                cardRepository.countByUserIdAndLastSeenAtIsNull(user.getId()));
    }
}
