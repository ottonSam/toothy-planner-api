package br.com.ottonsam.toothy_planner_api.financial_manager.dtos;

import java.util.List;

public record ExpenseAudioResponse(
        String transcribedText,
        ExpenseAudioClassificationResponse classification,
        ExpenseResponse expense,
        InstallmentExpenseResponse installmentExpense,
        RecurringExpenseResponse recurringExpense,
        List<ExpenseResponse> generatedExpenses) {

    public ExpenseAudioResponse {
        generatedExpenses = List.copyOf(generatedExpenses);
    }

    public static ExpenseAudioResponse from(String transcribedText, ExpenseTextResponse textResponse) {
        return new ExpenseAudioResponse(
                transcribedText,
                new ExpenseAudioClassificationResponse(textResponse.type()),
                textResponse.expense(),
                textResponse.installmentExpense(),
                textResponse.recurringExpense(),
                textResponse.generatedExpenses());
    }

    @Override
    public List<ExpenseResponse> generatedExpenses() {
        return List.copyOf(generatedExpenses);
    }
}
