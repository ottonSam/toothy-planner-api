package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseWalletEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseWalletResponse(
        UUID id,
        String description,
        BigDecimal spendingGoal,
        int cycleEndDay,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ExpenseWalletResponse from(ExpenseWalletEntity wallet) {
        return new ExpenseWalletResponse(
                wallet.getId(),
                wallet.getDescription(),
                wallet.getSpendingGoal(),
                wallet.getCycleEndDay(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt());
    }
}
