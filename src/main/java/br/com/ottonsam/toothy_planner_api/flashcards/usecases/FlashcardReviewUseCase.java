package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardAnswerRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardCardResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardNextCardRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardNextCardResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardReviewRatingResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardReviewAnswerEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardReviewRating;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardRepository;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardReviewAnswerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FlashcardReviewUseCase {

    private final FlashcardRepository cardRepository;
    private final FlashcardReviewAnswerRepository answerRepository;
    private final FlashcardDeckUseCase deckUseCase;
    private final FlashcardCardUseCase cardUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public FlashcardReviewUseCase(
            FlashcardRepository cardRepository,
            FlashcardReviewAnswerRepository answerRepository,
            FlashcardDeckUseCase deckUseCase,
            FlashcardCardUseCase cardUseCase,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.cardRepository = cardRepository;
        this.answerRepository = answerRepository;
        this.deckUseCase = deckUseCase;
        this.cardUseCase = cardUseCase;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public List<FlashcardReviewRatingResponse> ratings() {
        return Arrays.stream(FlashcardReviewRating.values())
                .map(FlashcardReviewRatingResponse::from)
                .toList();
    }

    public FlashcardNextCardResponse nextCard(FlashcardNextCardRequest request) {
        var user = currentUserProvider.get();
        var cards = request != null && request.deckId() != null
                ? cardsFromDeck(user.getId(), request.deckId())
                : cardRepository.findAllByUserIdAndActiveTrue(user.getId());
        var now = OffsetDateTime.now(clock);
        var scored = cards.stream()
                .map(card -> new ScoredCard(card, score(card, now)))
                .max(Comparator.comparing(ScoredCard::score))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No flashcards available for review"));
        scored.card().markSeen(now);
        cardRepository.save(scored.card());
        return new FlashcardNextCardResponse(
                FlashcardCardResponse.from(scored.card()),
                BigDecimal.valueOf(scored.score()).setScale(2, RoundingMode.HALF_UP));
    }

    public FlashcardCardResponse answer(UUID cardId, FlashcardAnswerRequest request) {
        var user = currentUserProvider.get();
        var rating = requiredRating(request);
        var card = cardUseCase.findOwned(cardId, user.getId());
        var now = OffsetDateTime.now(clock);
        card.answer(rating, now);
        cardRepository.save(card);
        answerRepository.save(FlashcardReviewAnswerEntity.create(card, rating, now));
        return FlashcardCardResponse.from(card);
    }

    double score(FlashcardEntity card, OffsetDateTime now) {
        var score = 0.0;
        if (card.getNextReviewAt() != null && !card.getNextReviewAt().isAfter(now)) {
            score += 40.0;
        }
        score += card.getWrongCount() * 3.0;
        score += card.getConsecutiveWrong() * 5.0;
        score += card.getDifficulty().doubleValue() * 2.0;
        score -= card.getConsecutiveCorrect() * 2.0;

        if (card.getLastSeenAt() == null) {
            score += 50.0;
            return score;
        }
        var sinceSeen = Duration.between(card.getLastSeenAt(), now);
        var daysSinceLastSeen = Math.min(30, Math.max(0, sinceSeen.toDays()));
        score += daysSinceLastSeen * 1.5;
        if (sinceSeen.compareTo(Duration.ofMinutes(10)) < 0) {
            score -= 50.0;
        } else if (card.getLastSeenAt().toLocalDate().isEqual(now.toLocalDate())) {
            score -= 10.0;
        }
        return score;
    }

    private List<FlashcardEntity> cardsFromDeck(UUID userId, UUID deckId) {
        deckUseCase.findOwned(deckId, userId);
        return cardRepository.findAllByUserIdAndDeckIdAndActiveTrue(userId, deckId);
    }

    private FlashcardReviewRating requiredRating(FlashcardAnswerRequest request) {
        if (request == null || request.rating() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard review rating is required");
        }
        return request.rating();
    }

    private record ScoredCard(FlashcardEntity card, double score) {}
}
