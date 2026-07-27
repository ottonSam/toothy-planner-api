package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardEntity;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleUnaryOperator;
import org.springframework.stereotype.Component;

@Component
public class FlashcardWeightedSelector {

    private final DoubleUnaryOperator randomDraw;

    public FlashcardWeightedSelector() {
        this(bound -> ThreadLocalRandom.current().nextDouble(bound));
    }

    FlashcardWeightedSelector(DoubleUnaryOperator randomDraw) {
        this.randomDraw = randomDraw;
    }

    WeightedFlashcardCandidate select(List<WeightedFlashcardCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Flashcard candidates are required");
        }

        var totalWeight = candidates.stream()
                .mapToDouble(WeightedFlashcardCandidate::score)
                .sum();
        var draw = randomDraw.applyAsDouble(totalWeight);
        var cumulativeWeight = 0.0;
        for (var candidate : candidates) {
            cumulativeWeight += candidate.score();
            if (draw < cumulativeWeight) {
                return candidate;
            }
        }
        return candidates.getLast();
    }
}

record WeightedFlashcardCandidate(FlashcardEntity card, double score) {

    WeightedFlashcardCandidate {
        if (card == null) {
            throw new IllegalArgumentException("Flashcard candidate is required");
        }
        if (!Double.isFinite(score) || score < 1.0) {
            throw new IllegalArgumentException("Flashcard candidate score must be at least one");
        }
    }
}
