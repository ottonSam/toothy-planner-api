package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseTextAiClient {

    List<ExpenseTextClassification> classify(UUID userId, String text, LocalDate referenceDate);
}
