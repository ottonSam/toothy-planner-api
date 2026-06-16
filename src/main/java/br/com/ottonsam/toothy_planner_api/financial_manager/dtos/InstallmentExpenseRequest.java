package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InstallmentExpenseRequest(
        UUID categoryId,
        String description,
        BigDecimal totalAmount,
        BigDecimal installmentAmount,
        Integer installments,
        LocalDate firstExpenseDate) {}
