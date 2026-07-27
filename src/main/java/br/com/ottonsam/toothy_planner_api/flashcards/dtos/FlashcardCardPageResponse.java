package br.com.ottonsam.toothy_planner_api.flashcards.dtos;

import java.util.List;
import org.springframework.data.domain.Page;

public record FlashcardCardPageResponse(
        List<FlashcardCardResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public FlashcardCardPageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    @Override
    public List<FlashcardCardResponse> content() {
        return List.copyOf(content);
    }

    public static FlashcardCardPageResponse from(Page<FlashcardCardResponse> cards) {
        return new FlashcardCardPageResponse(
                cards.getContent(),
                cards.getNumber(),
                cards.getSize(),
                cards.getTotalElements(),
                cards.getTotalPages(),
                cards.isFirst(),
                cards.isLast());
    }
}
