package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.util.UUID;

public record CancelRecurringExpenseRequest(UUID cycleId) {}
