package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCycleEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseCycleResponse(
        UUID id,
        UUID walletId,
        int referenceMonth,
        int referenceYear,
        LocalDate startsAt,
        LocalDate endsAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ExpenseCycleResponse from(ExpenseCycleEntity cycle) {
        return new ExpenseCycleResponse(
                cycle.getId(),
                cycle.getWallet().getId(),
                cycle.getReferenceMonth(),
                cycle.getReferenceYear(),
                cycle.getStartsAt(),
                cycle.getEndsAt(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt());
    }
}
