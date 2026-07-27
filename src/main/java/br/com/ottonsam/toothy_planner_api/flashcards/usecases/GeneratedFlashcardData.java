package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardExampleData;
import java.util.List;

public record GeneratedFlashcardData(
        String word,
        String baseVerb,
        String pastSimple,
        String pastParticiple,
        String expression,
        String translation,
        String phonetic,
        String level,
        List<String> tags,
        List<FlashcardExampleData> examples,
        String usageNote) {

    public GeneratedFlashcardData {
        tags = tags == null ? List.of() : List.copyOf(tags);
        examples = examples == null ? List.of() : List.copyOf(examples);
    }

    @Override
    public List<String> tags() {
        return List.copyOf(tags);
    }

    @Override
    public List<FlashcardExampleData> examples() {
        return List.copyOf(examples);
    }
}
