package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import java.util.List;

public record FlashcardCardRequest(
        String word,
        String baseVerb,
        String pastSimple,
        String pastParticiple,
        String expression,
        String translation,
        String phonetic,
        String level,
        String usageNote,
        Boolean active,
        List<FlashcardExampleRequest> examples,
        List<String> tags) {

    public FlashcardCardRequest {
        examples = examples == null ? null : List.copyOf(examples);
        tags = tags == null ? null : List.copyOf(tags);
    }

    @Override
    public List<FlashcardExampleRequest> examples() {
        return examples == null ? null : List.copyOf(examples);
    }

    @Override
    public List<String> tags() {
        return tags == null ? null : List.copyOf(tags);
    }
}
