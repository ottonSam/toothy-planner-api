package br.com.ottonsam.toothy_planner_api.ai_usage.usecases;

import br.com.ottonsam.toothy_planner_api.ai_usage.dtos.AiTokenUsageResponse;
import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentAiTokenUsageUseCase {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final AiTokenUsageUseCase tokenUsageUseCase;
    private final CurrentUserProvider currentUserProvider;

    public GetCurrentAiTokenUsageUseCase(
            AiTokenUsageUseCase tokenUsageUseCase, CurrentUserProvider currentUserProvider) {
        this.tokenUsageUseCase = tokenUsageUseCase;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public AiTokenUsageResponse execute() {
        var userId = currentUserProvider.get().getId();
        var monthlyUsage = tokenUsageUseCase.currentMonthlyUsage(userId);
        var limit = monthlyUsage.map(usage -> usage.getLimitTokens()).orElse(tokenUsageUseCase.monthlyLimit());
        var remaining = monthlyUsage.map(usage -> usage.remainingTokens()).orElse(limit);
        var percentage = BigDecimal.valueOf(remaining)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(limit), 2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(ONE_HUNDRED);
        return new AiTokenUsageResponse(percentage, tokenUsageUseCase.currentPeriodEndsAt(), remaining == 0);
    }
}
