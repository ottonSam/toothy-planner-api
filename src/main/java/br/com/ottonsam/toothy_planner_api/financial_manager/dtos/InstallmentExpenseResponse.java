package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.InstallmentExpenseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InstallmentExpenseResponse(
        UUID id,
        UUID walletId,
        ExpenseCategorySummaryResponse category,
        String description,
        BigDecimal totalAmount,
        BigDecimal installmentAmount,
        int installments,
        LocalDate firstExpenseDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static InstallmentExpenseResponse from(InstallmentExpenseEntity installmentExpense) {
        return new InstallmentExpenseResponse(
                installmentExpense.getId(),
                installmentExpense.getWallet().getId(),
                ExpenseCategorySummaryResponse.from(installmentExpense.getCategory()),
                installmentExpense.getDescription(),
                installmentExpense.getTotalAmount(),
                installmentExpense.getInstallmentAmount(),
                installmentExpense.getInstallments(),
                installmentExpense.getFirstExpenseDate(),
                installmentExpense.getCreatedAt(),
                installmentExpense.getUpdatedAt());
    }
}
