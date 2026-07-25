package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseAudioRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseAudioResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseTextRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseSource;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseAudioUseCase {

    private static final Set<String> SUPPORTED_CONTENT_TYPES =
            Set.of("audio/webm", "audio/ogg", "audio/wav", "audio/mpeg", "audio/mp4");

    private final ExpenseAudioTranscriptionClient transcriptionClient;
    private final ExpenseTextUseCase expenseTextUseCase;
    private final long maxAudioBytes;

    public ExpenseAudioUseCase(
            ExpenseAudioTranscriptionClient transcriptionClient,
            ExpenseTextUseCase expenseTextUseCase,
            @Value("${transcription.max-audio-bytes:10485760}") long maxAudioBytes) {
        this.transcriptionClient = transcriptionClient;
        this.expenseTextUseCase = expenseTextUseCase;
        this.maxAudioBytes = Math.max(1, maxAudioBytes);
    }

    public ExpenseAudioResponse create(UUID walletId, ExpenseAudioRequest request) {
        var audioBase64 = requiredAudio(request);
        var contentType = requiredSupportedContentType(request);
        validateAudioSize(audioBase64);

        var transcribedText = transcriptionClient.transcribe(audioBase64, contentType);
        if (transcribedText == null || transcribedText.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Audio transcription returned empty text");
        }

        var textResponse = expenseTextUseCase.create(
                walletId,
                new ExpenseTextRequest(transcribedText.trim(), request.referenceDate()),
                ExpenseSource.AI_AUDIO);
        return ExpenseAudioResponse.from(transcribedText.trim(), textResponse);
    }

    private String requiredAudio(ExpenseAudioRequest request) {
        if (request == null
                || request.audioBase64() == null
                || request.audioBase64().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Audio content is required");
        }
        return request.audioBase64().trim();
    }

    private String requiredSupportedContentType(ExpenseAudioRequest request) {
        if (request == null
                || request.contentType() == null
                || request.contentType().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Audio content type is required");
        }
        var contentType = request.contentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Audio content type is not supported");
        }
        return contentType;
    }

    private void validateAudioSize(String audioBase64) {
        byte[] audioContent;
        try {
            audioContent = Base64.getDecoder().decode(audioBase64);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Audio content must be valid Base64");
        }
        if (audioContent.length > maxAudioBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Audio content exceeds maximum size");
        }
    }
}
