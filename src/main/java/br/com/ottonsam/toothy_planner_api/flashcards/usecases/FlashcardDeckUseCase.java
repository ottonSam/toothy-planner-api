package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardDeckRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardDeckResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardDeckRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FlashcardDeckUseCase {

    private final FlashcardDeckRepository deckRepository;
    private final CurrentUserProvider currentUserProvider;

    public FlashcardDeckUseCase(FlashcardDeckRepository deckRepository, CurrentUserProvider currentUserProvider) {
        this.deckRepository = deckRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<FlashcardDeckResponse> list() {
        var user = currentUserProvider.get();
        return deckRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(FlashcardDeckResponse::from)
                .toList();
    }

    public FlashcardDeckResponse get(UUID deckId) {
        var user = currentUserProvider.get();
        return FlashcardDeckResponse.from(findOwned(deckId, user.getId()));
    }

    public FlashcardDeckResponse update(UUID deckId, FlashcardDeckRequest request) {
        var user = currentUserProvider.get();
        var deck = findOwned(deckId, user.getId());
        deck.update(
                request.name(), request.context(), request.targetLanguage(), request.baseLanguage(), request.type());
        return FlashcardDeckResponse.from(deckRepository.save(deck));
    }

    public void delete(UUID deckId) {
        var user = currentUserProvider.get();
        deckRepository.delete(findOwned(deckId, user.getId()));
    }

    FlashcardDeckEntity findOwned(UUID deckId, UUID userId) {
        if (deckId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard deck id is required");
        }
        return deckRepository
                .findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flashcard deck not found"));
    }
}
