package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseWalletEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseWalletResponse(
        UUID id,
        String description,
        BigDecimal spendingGoal,
        LocalDate startsAt,
        int targetSpendingDay,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ExpenseWalletResponse from(ExpenseWalletEntity wallet) {
        return new ExpenseWalletResponse(
                wallet.getId(),
                wallet.getDescription(),
                wallet.getSpendingGoal(),
                wallet.getStartsAt(),
                wallet.getTargetSpendingDay(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt());
    }
}
