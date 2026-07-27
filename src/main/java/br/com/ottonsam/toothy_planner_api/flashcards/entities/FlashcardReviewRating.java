package br.com.ottonsam.toothy_planner_api.flashcards.entities;

public enum FlashcardReviewRating {
    AGAIN("Again", "The answer was wrong"),
    HARD("Hard", "The answer was correct but difficult"),
    GOOD("Good", "The answer was correct"),
    EASY("Easy", "The answer was correct and easy");

    private final String name;
    private final String description;

    FlashcardReviewRating(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
