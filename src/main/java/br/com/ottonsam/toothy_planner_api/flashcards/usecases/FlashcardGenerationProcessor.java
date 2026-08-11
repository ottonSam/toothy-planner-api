package br.com.ottonsam.toothy_planner_api.flashcards.usecases;

import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardDeckType;
import br.com.ottonsam.toothy_planner_api.flashcards.entities.FlashcardGenerationStatus;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FlashcardGenerationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlashcardGenerationProcessor.class);
    private static final int AI_REQUEST_SIZE = 20;

    private final FlashcardGenerationStateUseCase stateUseCase;
    private final FlashcardGenerationAiClient aiClient;
    private final FlashcardCardUseCase cardUseCase;
    private final FlashcardTagUseCase tagUseCase;
    private final int maxRetriesPerRequest;

    public FlashcardGenerationProcessor(
            FlashcardGenerationStateUseCase stateUseCase,
            FlashcardGenerationAiClient aiClient,
            FlashcardCardUseCase cardUseCase,
            FlashcardTagUseCase tagUseCase,
            @Value("${flashcards.generation.max-retries-per-batch:2}") int maxRetriesPerRequest) {
        this.stateUseCase = stateUseCase;
        this.aiClient = aiClient;
        this.cardUseCase = cardUseCase;
        this.tagUseCase = tagUseCase;
        this.maxRetriesPerRequest = Math.max(0, maxRetriesPerRequest);
    }

    @Async
    public void process(UUID jobId) {
        var totalCreated = 0;
        try {
            var job = stateUseCase.startJob(jobId);
            for (var batch : stateUseCase.batches(jobId)) {
                if (batch.status() != FlashcardGenerationStatus.PENDING) {
                    totalCreated += batch.createdCount();
                    continue;
                }

                stateUseCase.startBatch(batch.batchId());
                var result = processBatch(job, batch);
                totalCreated += result.createdCount();

                if (result.errorMessage() != null) {
                    stateUseCase.failBatch(batch.batchId(), result.errorMessage(), result.createdCount());
                    stateUseCase.cancelPendingBatches(jobId);
                    stateUseCase.failJob(jobId, result.errorMessage(), totalCreated);
                    LOGGER.warn(
                            "Flashcard generation job {} stopped at batch {}: {}",
                            jobId,
                            batch.batchId(),
                            result.errorMessage());
                    return;
                }
                stateUseCase.completeBatch(batch.batchId(), result.createdCount());
            }
            stateUseCase.completeJob(jobId, totalCreated);
        } catch (RuntimeException exception) {
            var errorMessage = errorMessage(exception);
            LOGGER.error("Flashcard generation job {} failed unexpectedly: {}", jobId, errorMessage, exception);
            failJobSafely(jobId, errorMessage);
        }
    }

    private FlashcardBatchProcessingResult processBatch(
            FlashcardGenerationJobContext job, FlashcardGenerationBatchContext batch) {
        var created = 0;
        while (created < batch.requestedCount()) {
            var requestedInCall = Math.min(AI_REQUEST_SIZE, batch.requestedCount() - created);
            var result = generateWithRetries(job, batch.batchId(), requestedInCall);
            if (result.errorMessage() != null) {
                return new FlashcardBatchProcessingResult(created, result.errorMessage());
            }

            var createdInCall = persistGeneratedCards(job, result.generated(), requestedInCall);
            created += createdInCall;
            if (createdInCall == 0) {
                return new FlashcardBatchProcessingResult(
                        created, "DeepSeek flashcard generation did not return new unique cards");
            }
        }
        return new FlashcardBatchProcessingResult(created, null);
    }

    private FlashcardAiCallResult generateWithRetries(
            FlashcardGenerationJobContext job, UUID batchId, int requestedCount) {
        RuntimeException lastException = null;
        for (var attempt = 1; attempt <= maxRetriesPerRequest + 1; attempt++) {
            try {
                var alreadyGeneratedTerms = cardUseCase.targetTerms(job.deckId());
                var generated = aiClient.generate(
                        job.userId(),
                        new FlashcardGenerationAiRequest(
                                job.type(),
                                job.context(),
                                job.targetLanguage(),
                                job.baseLanguage(),
                                requestedCount,
                                alreadyGeneratedTerms));
                return new FlashcardAiCallResult(generated, null);
            } catch (RuntimeException exception) {
                lastException = exception;
                LOGGER.warn(
                        "Flashcard generation request failed for job {}, batch {}, attempt {}/{}: {}",
                        job.jobId(),
                        batchId,
                        attempt,
                        maxRetriesPerRequest + 1,
                        errorMessage(exception));
            }
        }
        return new FlashcardAiCallResult(null, errorMessage(lastException));
    }

    private int persistGeneratedCards(
            FlashcardGenerationJobContext job, FlashcardGenerationAiResult result, int requestedCount) {
        var created = 0;
        var termsCreatedInCall = new HashSet<String>();
        for (var generated : result.cards()) {
            var targetTerm = targetTerm(job.type(), generated);
            var normalized = normalize(targetTerm);
            if (normalized == null
                    || termsCreatedInCall.contains(normalized)
                    || cardUseCase.existsTargetTerm(job.type(), job.deckId(), targetTerm)) {
                continue;
            }
            cardUseCase.createGenerated(job.deckId(), generated, tagUseCase);
            termsCreatedInCall.add(normalized);
            created++;
            if (created >= requestedCount) {
                break;
            }
        }
        return created;
    }

    private void failJobSafely(UUID jobId, String errorMessage) {
        try {
            var createdCount = stateUseCase.createdCount(jobId);
            stateUseCase.failOpenBatches(jobId, errorMessage, createdCount);
            stateUseCase.failJob(jobId, errorMessage, createdCount);
        } catch (RuntimeException stateException) {
            LOGGER.error("Could not update failed flashcard generation job {}", jobId, stateException);
        }
    }

    private String targetTerm(FlashcardDeckType type, GeneratedFlashcardData generated) {
        return switch (type) {
            case VOCABULARY -> generated.word();
            case IRREGULAR_VERBS -> generated.baseVerb();
            case EXPRESSIONS -> generated.expression();
        };
    }

    private String normalize(String term) {
        if (term == null || term.trim().isEmpty()) {
            return null;
        }
        return term.trim().toLowerCase(Locale.ROOT);
    }

    private String errorMessage(RuntimeException exception) {
        if (exception == null
                || exception.getMessage() == null
                || exception.getMessage().isBlank()) {
            return "Flashcard generation failed";
        }
        return exception.getMessage();
    }
}

record FlashcardBatchProcessingResult(int createdCount, String errorMessage) {}

record FlashcardAiCallResult(FlashcardGenerationAiResult generated, String errorMessage) {}
