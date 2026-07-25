package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.time.LocalDate;

public record ExpenseTextRequest(String text, LocalDate referenceDate) {}
