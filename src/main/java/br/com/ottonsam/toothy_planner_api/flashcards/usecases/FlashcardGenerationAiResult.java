package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import java.util.List;

public record FlashcardGenerationAiResult(List<GeneratedFlashcardData> cards) {

    public FlashcardGenerationAiResult {
        cards = List.copyOf(cards);
    }

    @Override
    public List<GeneratedFlashcardData> cards() {
        return List.copyOf(cards);
    }
}
