package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardCardPageResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardCardRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardCardResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardExampleRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardExampleData;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardDeckRepository;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FlashcardCardUseCase {

    private final FlashcardRepository cardRepository;
    private final FlashcardDeckRepository deckRepository;
    private final FlashcardDeckUseCase deckUseCase;
    private final FlashcardTagUseCase tagUseCase;
    private final CurrentUserProvider currentUserProvider;

    public FlashcardCardUseCase(
            FlashcardRepository cardRepository,
            FlashcardDeckRepository deckRepository,
            FlashcardDeckUseCase deckUseCase,
            FlashcardTagUseCase tagUseCase,
            CurrentUserProvider currentUserProvider) {
        this.cardRepository = cardRepository;
        this.deckRepository = deckRepository;
        this.deckUseCase = deckUseCase;
        this.tagUseCase = tagUseCase;
        this.currentUserProvider = currentUserProvider;
    }

    public FlashcardCardPageResponse listByDeck(UUID deckId, int page, int size) {
        var user = currentUserProvider.get();
        deckUseCase.findOwned(deckId, user.getId());
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard page must be greater than or equal to zero");
        }
        if (size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard page size must be between 1 and 100");
        }
        var pageable =
                PageRequest.of(page, size, Sort.by("createdAt").ascending().and(Sort.by("id")));
        return FlashcardCardPageResponse.from(cardRepository
                .findAllByDeckIdAndUserId(deckId, user.getId(), pageable)
                .map(FlashcardCardResponse::from));
    }

    public FlashcardCardResponse create(UUID deckId, FlashcardCardRequest request) {
        var user = currentUserProvider.get();
        var deck = deckUseCase.findOwned(deckId, user.getId());
        var examples = examplesForCreate(request.examples());
        var tags = tagUseCase.findOrCreateAll(user, request.tags());
        var card =
                switch (deck.getType()) {
                    case VOCABULARY ->
                        FlashcardEntity.vocabulary(
                                deck,
                                request.word(),
                                request.translation(),
                                request.phonetic(),
                                request.level(),
                                request.usageNote(),
                                examples,
                                tags);
                    case IRREGULAR_VERBS ->
                        FlashcardEntity.irregularVerb(
                                deck,
                                request.baseVerb(),
                                request.pastSimple(),
                                request.pastParticiple(),
                                request.translation(),
                                request.phonetic(),
                                request.usageNote(),
                                examples,
                                tags);
                    case EXPRESSIONS ->
                        FlashcardEntity.expression(
                                deck,
                                request.expression(),
                                request.translation(),
                                request.phonetic(),
                                request.usageNote(),
                                examples,
                                tags);
                };
        return FlashcardCardResponse.from(cardRepository.save(card));
    }

    public FlashcardCardResponse get(UUID cardId) {
        var user = currentUserProvider.get();
        return FlashcardCardResponse.from(findOwned(cardId, user.getId()));
    }

    public FlashcardCardResponse update(UUID cardId, FlashcardCardRequest request) {
        var user = currentUserProvider.get();
        var card = findOwned(cardId, user.getId());
        var active = request.active() == null ? card.isActive() : request.active();
        var examples = request.examples() == null ? existingExamples(card) : examples(request.examples());
        var tags =
                request.tags() == null ? Set.copyOf(card.getTags()) : tagUseCase.findOrCreateAll(user, request.tags());
        switch (card.getType()) {
            case VOCABULARY ->
                card.updateVocabulary(
                        request.word(),
                        request.translation(),
                        request.phonetic(),
                        request.level(),
                        request.usageNote(),
                        examples,
                        tags,
                        active);
            case IRREGULAR_VERBS ->
                card.updateIrregularVerb(
                        request.baseVerb(),
                        request.pastSimple(),
                        request.pastParticiple(),
                        request.translation(),
                        request.phonetic(),
                        request.usageNote(),
                        examples,
                        tags,
                        active);
            case EXPRESSIONS ->
                card.updateExpression(
                        request.expression(),
                        request.translation(),
                        request.phonetic(),
                        request.usageNote(),
                        examples,
                        tags,
                        active);
        }
        return FlashcardCardResponse.from(cardRepository.save(card));
    }

    public void delete(UUID cardId) {
        var user = currentUserProvider.get();
        cardRepository.delete(findOwned(cardId, user.getId()));
    }

    FlashcardEntity createGenerated(UUID deckId, GeneratedFlashcardData generated, FlashcardTagUseCase tagUseCase) {
        var deck = deckRepository
                .findById(deckId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flashcard deck not found"));
        var tags = tagUseCase.findOrCreateAll(deck.getUser(), generated.tags());
        var card =
                switch (deck.getType()) {
                    case VOCABULARY ->
                        FlashcardEntity.vocabulary(
                                deck,
                                generated.word(),
                                generated.translation(),
                                generated.phonetic(),
                                generated.level(),
                                generated.usageNote(),
                                generated.examples(),
                                tags);
                    case IRREGULAR_VERBS ->
                        FlashcardEntity.irregularVerb(
                                deck,
                                generated.baseVerb(),
                                generated.pastSimple(),
                                generated.pastParticiple(),
                                generated.translation(),
                                generated.phonetic(),
                                generated.usageNote(),
                                generated.examples(),
                                tags);
                    case EXPRESSIONS ->
                        FlashcardEntity.expression(
                                deck,
                                generated.expression(),
                                generated.translation(),
                                generated.phonetic(),
                                generated.usageNote(),
                                generated.examples(),
                                tags);
                };
        return cardRepository.save(card);
    }

    boolean existsTargetTerm(FlashcardDeckType type, UUID deckId, String targetTerm) {
        if (targetTerm == null || targetTerm.trim().isEmpty()) {
            return true;
        }
        return switch (type) {
            case VOCABULARY -> cardRepository.existsByDeckIdAndWordIgnoreCase(deckId, targetTerm.trim());
            case IRREGULAR_VERBS -> cardRepository.existsByDeckIdAndBaseVerbIgnoreCase(deckId, targetTerm.trim());
            case EXPRESSIONS -> cardRepository.existsByDeckIdAndExpressionIgnoreCase(deckId, targetTerm.trim());
        };
    }

    List<String> targetTerms(UUID deckId) {
        return cardRepository.findAllByDeckIdOrderByCreatedAtAsc(deckId).stream()
                .map(FlashcardEntity::targetTerm)
                .filter(term -> term != null && !term.isBlank())
                .map(term -> term.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    FlashcardEntity findOwned(UUID cardId, UUID userId) {
        if (cardId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard id is required");
        }
        return cardRepository
                .findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flashcard not found"));
    }

    private List<FlashcardExampleData> examplesForCreate(List<FlashcardExampleRequest> examples) {
        return examples == null ? List.of() : examples(examples);
    }

    private List<FlashcardExampleData> examples(List<FlashcardExampleRequest> examples) {
        return examples.stream()
                .map(example -> new FlashcardExampleData(example.text(), example.translation()))
                .toList();
    }

    private List<FlashcardExampleData> existingExamples(FlashcardEntity card) {
        return card.getExamples().stream()
                .map(example -> new FlashcardExampleData(example.getText(), example.getTranslation()))
                .toList();
    }
}
