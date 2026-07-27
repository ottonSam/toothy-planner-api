package br.com.ottonsam.toothy_planner_api.flashcards.controllers;

import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardAnswerRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardCardPageResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardCardRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardCardResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardDeckGenerateRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardDeckRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardDeckResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardGenerationJobResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardMetricsResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardNextCardRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardNextCardResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardReviewRatingResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardTagResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.FlashcardCardUseCase;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.FlashcardDeckUseCase;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.FlashcardGenerationUseCase;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.FlashcardMetricsUseCase;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.FlashcardReviewUseCase;
import br.com.ottonsam.toothy_planner_api.flashcards.usecases.FlashcardTagUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flashcards")
public class FlashcardController {

    private final FlashcardGenerationUseCase generationUseCase;
    private final FlashcardDeckUseCase deckUseCase;
    private final FlashcardCardUseCase cardUseCase;
    private final FlashcardTagUseCase tagUseCase;
    private final FlashcardReviewUseCase reviewUseCase;
    private final FlashcardMetricsUseCase metricsUseCase;

    public FlashcardController(
            FlashcardGenerationUseCase generationUseCase,
            FlashcardDeckUseCase deckUseCase,
            FlashcardCardUseCase cardUseCase,
            FlashcardTagUseCase tagUseCase,
            FlashcardReviewUseCase reviewUseCase,
            FlashcardMetricsUseCase metricsUseCase) {
        this.generationUseCase = generationUseCase;
        this.deckUseCase = deckUseCase;
        this.cardUseCase = cardUseCase;
        this.tagUseCase = tagUseCase;
        this.reviewUseCase = reviewUseCase;
        this.metricsUseCase = metricsUseCase;
    }

    @PostMapping("/decks/generate")
    ResponseEntity<FlashcardGenerationJobResponse> generateDeck(@RequestBody FlashcardDeckGenerateRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(generationUseCase.generate(request));
    }

    @GetMapping("/generation-jobs/{jobId}")
    FlashcardGenerationJobResponse getGenerationJob(@PathVariable UUID jobId) {
        return generationUseCase.get(jobId);
    }

    @GetMapping("/decks/{deckId}/generation-status")
    FlashcardGenerationJobResponse getDeckGenerationStatus(@PathVariable UUID deckId) {
        return generationUseCase.getByDeckId(deckId);
    }

    @GetMapping("/decks")
    List<FlashcardDeckResponse> listDecks() {
        return deckUseCase.list();
    }

    @GetMapping("/decks/{deckId}")
    FlashcardDeckResponse getDeck(@PathVariable UUID deckId) {
        return deckUseCase.get(deckId);
    }

    @PutMapping("/decks/{deckId}")
    FlashcardDeckResponse updateDeck(@PathVariable UUID deckId, @RequestBody FlashcardDeckRequest request) {
        return deckUseCase.update(deckId, request);
    }

    @DeleteMapping("/decks/{deckId}")
    ResponseEntity<Void> deleteDeck(@PathVariable UUID deckId) {
        deckUseCase.delete(deckId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/decks/{deckId}/cards")
    FlashcardCardPageResponse listDeckCards(
            @PathVariable UUID deckId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return cardUseCase.listByDeck(deckId, page, size);
    }

    @PostMapping("/decks/{deckId}/cards")
    ResponseEntity<FlashcardCardResponse> createCard(
            @PathVariable UUID deckId, @RequestBody FlashcardCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardUseCase.create(deckId, request));
    }

    @GetMapping("/cards/{cardId}")
    FlashcardCardResponse getCard(@PathVariable UUID cardId) {
        return cardUseCase.get(cardId);
    }

    @PutMapping("/cards/{cardId}")
    FlashcardCardResponse updateCard(@PathVariable UUID cardId, @RequestBody FlashcardCardRequest request) {
        return cardUseCase.update(cardId, request);
    }

    @DeleteMapping("/cards/{cardId}")
    ResponseEntity<Void> deleteCard(@PathVariable UUID cardId) {
        cardUseCase.delete(cardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tags")
    List<FlashcardTagResponse> listTags() {
        return tagUseCase.list();
    }

    @GetMapping("/review-ratings")
    List<FlashcardReviewRatingResponse> reviewRatings() {
        return reviewUseCase.ratings();
    }

    @PostMapping("/review/next-card")
    FlashcardNextCardResponse nextCard(@RequestBody(required = false) FlashcardNextCardRequest request) {
        return reviewUseCase.nextCard(request);
    }

    @PostMapping("/cards/{cardId}/answer")
    FlashcardCardResponse answerCard(@PathVariable UUID cardId, @RequestBody FlashcardAnswerRequest request) {
        return reviewUseCase.answer(cardId, request);
    }

    @GetMapping("/metrics")
    FlashcardMetricsResponse metrics() {
        return metricsUseCase.metrics();
    }
}
