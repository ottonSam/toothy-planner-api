package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

public interface FlashcardGenerationAiClient {

    FlashcardGenerationAiResult generate(FlashcardGenerationAiRequest request);
}
