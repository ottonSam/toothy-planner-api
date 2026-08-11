package br.com.ottonsam.toothy_planner_api.ai_usage.usecases;

import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiFeature;
import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiMonthlyTokenUsageEntity;
import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiTokenUsageEventEntity;
import br.com.ottonsam.toothy_planner_api.ai_usage.entities.AiTokenUsageEventStatus;
import br.com.ottonsam.toothy_planner_api.ai_usage.repositories.AiMonthlyTokenUsageRepository;
import br.com.ottonsam.toothy_planner_api.ai_usage.repositories.AiTokenUsageEventRepository;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiTokenUsageUseCase {

    private static final int CHAT_FORMAT_MARGIN_TOKENS = 256;

    private final AiMonthlyTokenUsageRepository monthlyRepository;
    private final AiTokenUsageEventRepository eventRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final long monthlyLimit;
    private final long reservationTimeoutMinutes;

    public AiTokenUsageUseCase(
            AiMonthlyTokenUsageRepository monthlyRepository,
            AiTokenUsageEventRepository eventRepository,
            UserRepository userRepository,
            Clock clock,
            @Value("${ai.monthly-token-limit:500000}") long monthlyLimit,
            @Value("${ai.reservation-timeout-minutes:10}") long reservationTimeoutMinutes) {
        this.monthlyRepository = monthlyRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.monthlyLimit = Math.max(1, monthlyLimit);
        this.reservationTimeoutMinutes = Math.max(1, reservationTimeoutMinutes);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID reserve(UUID userId, AiFeature feature, String serializedRequest, int maxOutputTokens) {
        if (serializedRequest == null || feature == null || maxOutputTokens <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI token usage reservation is invalid");
        }
        var user = lockUser(userId);
        var monthlyUsage = currentMonthlyUsage(user.getId())
                .orElseGet(() -> monthlyRepository.save(
                        AiMonthlyTokenUsageEntity.create(user, currentPeriodStart(), monthlyLimit, now())));
        chargeStaleReservations(monthlyUsage);
        var requestBytes = serializedRequest.getBytes(StandardCharsets.UTF_8).length;
        var reservedTokens =
                Math.addExact(Math.addExact((long) requestBytes, maxOutputTokens), CHAT_FORMAT_MARGIN_TOKENS);
        monthlyUsage.reserve(reservedTokens, now());
        monthlyRepository.save(monthlyUsage);
        return eventRepository
                .save(AiTokenUsageEventEntity.reserve(user, monthlyUsage, feature, reservedTokens, now()))
                .getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void charge(UUID reservationId, JsonNode usage) {
        var event = lockedEvent(reservationId);
        if (!event.isReserved()) {
            return;
        }
        var promptTokens = nonNegativeLong(usage, "prompt_tokens");
        var completionTokens = nonNegativeLong(usage, "completion_tokens");
        var totalTokens = nonNegativeLong(usage, "total_tokens");
        if (totalTokens == null) {
            chargeReserved(event);
            return;
        }
        var monthlyUsage = event.getMonthlyUsage();
        monthlyUsage.charge(event.getReservedTokens(), totalTokens, now());
        event.charge(
                promptTokens == null ? 0 : promptTokens,
                completionTokens == null
                        ? Math.max(0, totalTokens - (promptTokens == null ? 0 : promptTokens))
                        : completionTokens,
                totalTokens,
                now());
        monthlyRepository.save(monthlyUsage);
        eventRepository.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void chargeReserved(UUID reservationId) {
        var event = lockedEvent(reservationId);
        if (event.isReserved()) {
            chargeReserved(event);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(UUID reservationId) {
        var event = lockedEvent(reservationId);
        if (!event.isReserved()) {
            return;
        }
        var monthlyUsage = event.getMonthlyUsage();
        monthlyUsage.release(event.getReservedTokens(), now());
        event.release(now());
        monthlyRepository.save(monthlyUsage);
        eventRepository.save(event);
    }

    java.util.Optional<AiMonthlyTokenUsageEntity> currentMonthlyUsage(UUID userId) {
        return monthlyRepository.findByUserIdAndPeriodStart(userId, currentPeriodStart());
    }

    LocalDate currentPeriodStart() {
        return YearMonth.now(clock.withZone(ZoneOffset.UTC)).atDay(1);
    }

    OffsetDateTime currentPeriodEndsAt() {
        return currentPeriodStart().plusMonths(1).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    long monthlyLimit() {
        return monthlyLimit;
    }

    private void chargeReserved(AiTokenUsageEventEntity event) {
        var monthlyUsage = event.getMonthlyUsage();
        monthlyUsage.charge(event.getReservedTokens(), event.getReservedTokens(), now());
        event.chargeReserved(now());
        monthlyRepository.save(monthlyUsage);
        eventRepository.save(event);
    }

    private void chargeStaleReservations(AiMonthlyTokenUsageEntity monthlyUsage) {
        var staleEvents = eventRepository.findAllByMonthlyUsageIdAndStatusAndCreatedAtBefore(
                monthlyUsage.getId(), AiTokenUsageEventStatus.RESERVED, now().minusMinutes(reservationTimeoutMinutes));
        if (staleEvents.isEmpty()) {
            return;
        }
        for (var event : staleEvents) {
            monthlyUsage.charge(event.getReservedTokens(), event.getReservedTokens(), now());
            event.chargeReserved(now());
        }
        monthlyRepository.save(monthlyUsage);
        eventRepository.saveAll(staleEvents);
    }

    private AiTokenUsageEventEntity lockedEvent(UUID reservationId) {
        if (reservationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI token usage reservation id is required");
        }
        var existing = eventRepository
                .findById(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AI token usage reservation not found"));
        lockUser(existing.getUser().getId());
        return eventRepository
                .findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AI token usage reservation not found"));
    }

    private br.com.ottonsam.toothy_planner_api.user.entities.UserEntity lockUser(UUID userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AI token usage user id is required");
        }
        return userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Long nonNegativeLong(JsonNode usage, String field) {
        if (usage == null || !usage.path(field).canConvertToLong()) {
            return null;
        }
        var value = usage.path(field).longValue();
        return value < 0 ? null : value;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
