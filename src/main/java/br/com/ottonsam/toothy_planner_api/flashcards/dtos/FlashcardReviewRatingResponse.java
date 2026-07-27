package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardReviewRating;

public record FlashcardReviewRatingResponse(String key, String name, String description) {

    public static FlashcardReviewRatingResponse from(FlashcardReviewRating rating) {
        return new FlashcardReviewRatingResponse(rating.name(), rating.getName(), rating.getDescription());
    }
}
