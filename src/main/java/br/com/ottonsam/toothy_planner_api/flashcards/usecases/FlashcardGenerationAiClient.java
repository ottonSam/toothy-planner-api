package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import java.util.UUID;

public interface FlashcardGenerationAiClient {

    FlashcardGenerationAiResult generate(UUID userId, FlashcardGenerationAiRequest request);
}
