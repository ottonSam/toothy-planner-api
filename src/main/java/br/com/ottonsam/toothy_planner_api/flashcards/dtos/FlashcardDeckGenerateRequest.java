package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;

public record FlashcardDeckGenerateRequest(
        String name,
        String context,
        String targetLanguage,
        String baseLanguage,
        FlashcardDeckType type,
        Integer cardCount) {}
