package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardDeckGenerateRequest;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardGenerationBatchResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.dtos.FlashcardGenerationJobResponse;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationBatchEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationJobEntity;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardDeckRepository;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardGenerationBatchRepository;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardGenerationJobRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class FlashcardGenerationUseCase {

    private static final int BATCH_SIZE = 100;

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardGenerationJobRepository jobRepository;
    private final FlashcardGenerationBatchRepository batchRepository;
    private final FlashcardGenerationProcessor processor;
    private final CurrentUserProvider currentUserProvider;
    private final TransactionTemplate transactionTemplate;
    private final int maxCardsPerJob;

    public FlashcardGenerationUseCase(
            FlashcardDeckRepository deckRepository,
            FlashcardGenerationJobRepository jobRepository,
            FlashcardGenerationBatchRepository batchRepository,
            FlashcardGenerationProcessor processor,
            CurrentUserProvider currentUserProvider,
            PlatformTransactionManager transactionManager,
            @Value("${flashcards.generation.max-cards-per-job:1000}") int maxCardsPerJob) {
        this.deckRepository = deckRepository;
        this.jobRepository = jobRepository;
        this.batchRepository = batchRepository;
        this.processor = processor;
        this.currentUserProvider = currentUserProvider;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.maxCardsPerJob = Math.max(1, maxCardsPerJob);
    }

    public FlashcardGenerationJobResponse generate(FlashcardDeckGenerateRequest request) {
        validateRequest(request);
        var response = transactionTemplate.execute(status -> {
            var user = currentUserProvider.get();
            var deck = deckRepository.save(FlashcardDeckEntity.create(
                    user,
                    request.name(),
                    request.context(),
                    request.targetLanguage(),
                    request.baseLanguage(),
                    request.type()));
            var job = jobRepository.save(FlashcardGenerationJobEntity.create(deck, request.cardCount()));
            var batches = batchRepository.saveAll(createBatches(job, request.cardCount()));
            return FlashcardGenerationJobResponse.from(
                    job,
                    batches.stream().map(FlashcardGenerationBatchResponse::from).toList());
        });
        processor.process(response.id());
        return response;
    }

    @Transactional(readOnly = true)
    public FlashcardGenerationJobResponse get(UUID jobId) {
        var user = currentUserProvider.get();
        var job = jobRepository
                .findByIdAndUserId(jobId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flashcard generation job not found"));
        return response(job);
    }

    @Transactional(readOnly = true)
    public FlashcardGenerationJobResponse getByDeckId(UUID deckId) {
        var user = currentUserProvider.get();
        var job = jobRepository
                .findFirstByDeckIdAndUserIdOrderByCreatedAtDesc(deckId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flashcard generation job not found"));
        return response(job);
    }

    private FlashcardGenerationJobResponse response(FlashcardGenerationJobEntity job) {
        var batches = batchRepository.findAllByJobIdOrderByBatchNumberAsc(job.getId()).stream()
                .map(FlashcardGenerationBatchResponse::from)
                .toList();
        return FlashcardGenerationJobResponse.from(job, batches);
    }

    private void validateRequest(FlashcardDeckGenerateRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard generation request is required");
        }
        if (request.cardCount() == null || request.cardCount() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard generation card count must be greater than zero");
        }
        if (request.cardCount() > maxCardsPerJob) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Flashcard generation card count exceeds maximum size");
        }
    }

    private List<FlashcardGenerationBatchEntity> createBatches(FlashcardGenerationJobEntity job, int cardCount) {
        var batches = new ArrayList<FlashcardGenerationBatchEntity>();
        var remaining = cardCount;
        var batchNumber = 1;
        while (remaining > 0) {
            var requested = Math.min(BATCH_SIZE, remaining);
            batches.add(FlashcardGenerationBatchEntity.create(job, batchNumber, requested));
            remaining -= requested;
            batchNumber++;
        }
        return batches;
    }
}
