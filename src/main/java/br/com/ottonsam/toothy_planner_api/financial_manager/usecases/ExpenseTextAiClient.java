package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import java.time.LocalDate;

public interface ExpenseTextAiClient {

    ExpenseTextClassification classify(String text, LocalDate referenceDate);
}
