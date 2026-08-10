package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseTextAiClient {

    List<ExpenseTextClassification> classify(String text, LocalDate referenceDate);
}
