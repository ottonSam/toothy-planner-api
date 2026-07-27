package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardTagEntity;
import java.util.UUID;

public record FlashcardTagResponse(UUID id, String name) {

    public static FlashcardTagResponse from(FlashcardTagEntity tag) {
        return new FlashcardTagResponse(tag.getId(), tag.getName());
    }
}
