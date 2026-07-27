package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;

public record FlashcardDeckRequest(
        String name, String context, String targetLanguage, String baseLanguage, FlashcardDeckType type) {}
