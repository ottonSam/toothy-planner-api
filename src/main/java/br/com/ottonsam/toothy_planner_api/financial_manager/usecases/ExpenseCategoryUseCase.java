package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCategoryResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategory;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExpenseCategoryUseCase {

    public List<ExpenseCategoryResponse> list() {
        return Arrays.stream(ExpenseCategory.values())
                .map(ExpenseCategoryResponse::from)
                .toList();
    }

    ExpenseCategory required(ExpenseCategory category) {
        if (category == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense category is required");
        }
        return category;
    }
}
