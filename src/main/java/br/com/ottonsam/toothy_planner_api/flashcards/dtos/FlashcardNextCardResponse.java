package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import java.math.BigDecimal;

public record FlashcardNextCardResponse(FlashcardCardResponse card, BigDecimal score) {}
