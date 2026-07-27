package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import java.util.List;

public record FlashcardGenerationAiRequest(
        FlashcardDeckType type,
        String context,
        String targetLanguage,
        String baseLanguage,
        int requestedCount,
        List<String> alreadyGeneratedTerms) {

    public FlashcardGenerationAiRequest {
        alreadyGeneratedTerms = List.copyOf(alreadyGeneratedTerms);
    }

    @Override
    public List<String> alreadyGeneratedTerms() {
        return List.copyOf(alreadyGeneratedTerms);
    }
}
