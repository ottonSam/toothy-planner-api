package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import java.math.BigDecimal;

public record FlashcardMetricsResponse(
        long totalDecks,
        long activeCards,
        long reviewedToday,
        long reviewedThisWeek,
        long totalCorrect,
        long totalWrong,
        BigDecimal accuracyRate,
        long dueCards,
        long neverSeenCards) {}
