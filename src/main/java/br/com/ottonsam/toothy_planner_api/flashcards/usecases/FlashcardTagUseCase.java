package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardTagResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardTagEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardTagRepository;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FlashcardTagUseCase {

    private final FlashcardTagRepository tagRepository;
    private final CurrentUserProvider currentUserProvider;

    public FlashcardTagUseCase(FlashcardTagRepository tagRepository, CurrentUserProvider currentUserProvider) {
        this.tagRepository = tagRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<FlashcardTagResponse> list() {
        var user = currentUserProvider.get();
        return tagRepository.findAllByUserIdOrderByNameAsc(user.getId()).stream()
                .map(FlashcardTagResponse::from)
                .toList();
    }

    Set<FlashcardTagEntity> findOrCreateAll(UserEntity user, List<String> tagNames) {
        var tags = new LinkedHashSet<FlashcardTagEntity>();
        if (tagNames == null) {
            return tags;
        }
        for (String tagName : tagNames) {
            if (tagName == null || tagName.trim().isEmpty()) {
                continue;
            }
            var normalized = FlashcardTagEntity.normalize(tagName);
            var tag = tagRepository
                    .findByUserIdAndNameNormalized(user.getId(), normalized)
                    .orElseGet(() -> tagRepository.save(FlashcardTagEntity.create(user, tagName)));
            tags.add(tag);
        }
        return tags;
    }
}
