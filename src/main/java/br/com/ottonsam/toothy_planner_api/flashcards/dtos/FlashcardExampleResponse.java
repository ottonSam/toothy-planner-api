package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardExampleEntity;

public record FlashcardExampleResponse(String text, String translation) {

    public static FlashcardExampleResponse from(FlashcardExampleEntity example) {
        return new FlashcardExampleResponse(example.getText(), example.getTranslation());
    }
}
