package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCategoryRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCategoryResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategoryEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseCategoryRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.InstallmentExpenseRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.RecurringExpenseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseCategoryUseCase {

    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final InstallmentExpenseRepository installmentExpenseRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final CurrentUserProvider currentUserProvider;

    public ExpenseCategoryUseCase(
            ExpenseCategoryRepository categoryRepository,
            ExpenseRepository expenseRepository,
            InstallmentExpenseRepository installmentExpenseRepository,
            RecurringExpenseRepository recurringExpenseRepository,
            CurrentUserProvider currentUserProvider) {
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
        this.installmentExpenseRepository = installmentExpenseRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public ExpenseCategoryResponse create(ExpenseCategoryRequest request) {
        var user = currentUserProvider.get();
        ensureNameIsUnique(request.name(), user.getId());
        var category = ExpenseCategoryEntity.create(request.name(), request.color(), request.icon(), user);
        return ExpenseCategoryResponse.from(categoryRepository.save(category));
    }

    public List<ExpenseCategoryResponse> list() {
        var user = currentUserProvider.get();
        return categoryRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(ExpenseCategoryResponse::from)
                .toList();
    }

    public ExpenseCategoryResponse get(UUID id) {
        var user = currentUserProvider.get();
        return ExpenseCategoryResponse.from(findOwned(id, user.getId()));
    }

    public ExpenseCategoryResponse update(UUID id, ExpenseCategoryRequest request) {
        var user = currentUserProvider.get();
        var category = findOwned(id, user.getId());
        ensureNameIsUniqueForUpdate(request.name(), user.getId(), id);
        category.update(request.name(), request.color(), request.icon());
        return ExpenseCategoryResponse.from(categoryRepository.save(category));
    }

    public void delete(UUID id) {
        var user = currentUserProvider.get();
        var category = findOwned(id, user.getId());
        if (expenseRepository.existsByCategoryId(id)
                || installmentExpenseRepository.existsByCategoryId(id)
                || recurringExpenseRepository.existsByCategoryId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category is associated with expenses");
        }
        categoryRepository.delete(category);
    }

    ExpenseCategoryEntity findOwned(UUID id, UUID userId) {
        if (id == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category id is required");
        }
        return categoryRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private void ensureNameIsUnique(String name, UUID userId) {
        ExpenseCategoryEntity.validateName(name);
        if (categoryRepository.existsByNameIgnoreCaseAndUserId(name.trim(), userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category name already exists");
        }
    }

    private void ensureNameIsUniqueForUpdate(String name, UUID userId, UUID id) {
        ExpenseCategoryEntity.validateName(name);
        if (categoryRepository.existsByNameIgnoreCaseAndUserIdAndIdNot(name.trim(), userId, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category name already exists");
        }
    }
}
