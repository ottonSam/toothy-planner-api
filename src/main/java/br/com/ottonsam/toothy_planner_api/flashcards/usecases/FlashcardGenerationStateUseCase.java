package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationStatus;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardGenerationBatchRepository;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardGenerationJobRepository;
import br.com.ottonsam.toothy_planner_api.flashcards.repositories.FlashcardRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FlashcardGenerationStateUseCase {

    private static final String CANCELED_BATCH_MESSAGE = "Generation stopped after a previous batch failed";

    private final FlashcardGenerationJobRepository jobRepository;
    private final FlashcardGenerationBatchRepository batchRepository;
    private final FlashcardRepository cardRepository;
    private final Clock clock;

    public FlashcardGenerationStateUseCase(
            FlashcardGenerationJobRepository jobRepository,
            FlashcardGenerationBatchRepository batchRepository,
            FlashcardRepository cardRepository,
            Clock clock) {
        this.jobRepository = jobRepository;
        this.batchRepository = batchRepository;
        this.cardRepository = cardRepository;
        this.clock = clock;
    }

    public FlashcardGenerationJobContext startJob(UUID jobId) {
        var job = jobRepository.findById(jobId).orElseThrow();
        job.start(now());
        jobRepository.save(job);
        return new FlashcardGenerationJobContext(
                job.getId(),
                job.getDeck().getId(),
                job.getType(),
                job.getContext(),
                job.getTargetLanguage(),
                job.getBaseLanguage());
    }

    @Transactional(readOnly = true)
    public List<FlashcardGenerationBatchContext> batches(UUID jobId) {
        return batchRepository.findAllByJobIdOrderByBatchNumberAsc(jobId).stream()
                .map(batch -> new FlashcardGenerationBatchContext(
                        batch.getId(), batch.getRequestedCount(), batch.getCreatedCount(), batch.getStatus()))
                .toList();
    }

    public void startBatch(UUID batchId) {
        var batch = batchRepository.findById(batchId).orElseThrow();
        batch.start(now());
        batchRepository.save(batch);
    }

    public void completeBatch(UUID batchId, int createdCount) {
        var batch = batchRepository.findById(batchId).orElseThrow();
        batch.complete(createdCount, now());
        batchRepository.save(batch);
    }

    public void failBatch(UUID batchId, String errorMessage, int createdCount) {
        var batch = batchRepository.findById(batchId).orElseThrow();
        batch.fail(errorMessage, createdCount, now());
        batchRepository.save(batch);
    }

    public void cancelPendingBatches(UUID jobId) {
        var canceledAt = now();
        var batches = batchRepository.findAllByJobIdOrderByBatchNumberAsc(jobId);
        batches.forEach(batch -> batch.cancel(CANCELED_BATCH_MESSAGE, canceledAt));
        batchRepository.saveAll(batches);
    }

    public void failOpenBatches(UUID jobId, String errorMessage, int totalCreatedCount) {
        var failedAt = now();
        var batches = batchRepository.findAllByJobIdOrderByBatchNumberAsc(jobId);
        var completedCount = batches.stream()
                .filter(batch -> batch.getStatus() == FlashcardGenerationStatus.COMPLETED
                        || batch.getStatus() == FlashcardGenerationStatus.PARTIAL_COMPLETED)
                .mapToInt(batch -> batch.getCreatedCount())
                .sum();
        var runningCount = Math.max(0, totalCreatedCount - completedCount);
        batches.forEach(batch -> {
            if (batch.getStatus() == FlashcardGenerationStatus.RUNNING) {
                batch.fail(errorMessage, runningCount, failedAt);
            } else {
                batch.cancel(CANCELED_BATCH_MESSAGE, failedAt);
            }
        });
        batchRepository.saveAll(batches);
    }

    public void completeJob(UUID jobId, int createdCount) {
        var job = jobRepository.findById(jobId).orElseThrow();
        job.complete(createdCount, now());
        jobRepository.save(job);
    }

    public void failJob(UUID jobId, String errorMessage, int createdCount) {
        var job = jobRepository.findById(jobId).orElseThrow();
        job.fail(errorMessage, createdCount, now());
        jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public int createdCount(UUID jobId) {
        var job = jobRepository.findById(jobId).orElseThrow();
        return Math.toIntExact(cardRepository.countByDeckId(job.getDeck().getId()));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}

record FlashcardGenerationJobContext(
        UUID jobId, UUID deckId, FlashcardDeckType type, String context, String targetLanguage, String baseLanguage) {}

record FlashcardGenerationBatchContext(
        UUID batchId, int requestedCount, int createdCount, FlashcardGenerationStatus status) {}
