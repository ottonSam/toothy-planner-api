package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseType;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseCycleRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.RecurringExpenseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseUseCase {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCycleRepository cycleRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseWalletUseCase walletUseCase;
    private final ExpenseCategoryUseCase categoryUseCase;
    private final ExpenseCycleService cycleService;
    private final CurrentUserProvider currentUserProvider;

    public ExpenseUseCase(
            ExpenseRepository expenseRepository,
            ExpenseCycleRepository cycleRepository,
            RecurringExpenseRepository recurringExpenseRepository,
            ExpenseWalletUseCase walletUseCase,
            ExpenseCategoryUseCase categoryUseCase,
            ExpenseCycleService cycleService,
            CurrentUserProvider currentUserProvider) {
        this.expenseRepository = expenseRepository;
        this.cycleRepository = cycleRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.walletUseCase = walletUseCase;
        this.categoryUseCase = categoryUseCase;
        this.cycleService = cycleService;
        this.currentUserProvider = currentUserProvider;
    }

    public ExpenseResponse create(UUID walletId, ExpenseRequest request) {
        var user = currentUserProvider.get();
        var wallet = walletUseCase.findOwned(walletId, user.getId());
        var category = categoryUseCase.findOwned(request.categoryId(), user.getId());
        var expenseDate = requiredExpenseDate(request);
        var cycle = cycleService.findOrCreateByDate(wallet, expenseDate);
        var expense =
                ExpenseEntity.oneTime(wallet, cycle, category, request.description(), request.amount(), expenseDate);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    public List<ExpenseResponse> list(UUID walletId) {
        var user = currentUserProvider.get();
        walletUseCase.findOwned(walletId, user.getId());
        return expenseRepository
                .findAllByWalletIdAndWalletUserIdOrderByExpenseDateAscCreatedAtAsc(walletId, user.getId())
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    public List<ExpenseResponse> listByCycle(UUID walletId, UUID cycleId) {
        var user = currentUserProvider.get();
        walletUseCase.findOwned(walletId, user.getId());
        cycleRepository
                .findByIdAndWalletIdAndWalletUserId(cycleId, walletId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cycle not found"));
        return expenseRepository
                .findAllByCycleIdAndWalletIdAndWalletUserIdOrderByExpenseDateAscCreatedAtAsc(
                        cycleId, walletId, user.getId())
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    public ExpenseResponse get(UUID walletId, UUID expenseId) {
        var user = currentUserProvider.get();
        return ExpenseResponse.from(findOwned(walletId, expenseId, user.getId()));
    }

    public ExpenseResponse update(UUID walletId, UUID expenseId, ExpenseRequest request) {
        var user = currentUserProvider.get();
        var expense = findOwned(walletId, expenseId, user.getId());
        var category = categoryUseCase.findOwned(request.categoryId(), user.getId());
        var expenseDate = requiredExpenseDate(request);
        var cycle = cycleService.findOrCreateByDate(expense.getWallet(), expenseDate);
        expense.update(cycle, category, request.description(), request.amount(), expenseDate);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    public void delete(UUID walletId, UUID expenseId) {
        var user = currentUserProvider.get();
        var expense = findOwned(walletId, expenseId, user.getId());
        if (expense.getType() != ExpenseType.RECURRING) {
            expenseRepository.delete(expense);
            return;
        }
        var recurrenceId = expense.getRecurrenceId();
        var recurringExpense = recurringExpenseRepository
                .findByIdAndWalletIdAndWalletUserId(recurrenceId, walletId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recurring expense not found"));
        recurringExpense.cancel(expense.getCycle().getStartsAt());
        expenseRepository.deleteAll(expenseRepository.findAllByRecurrenceIdAndCycle_StartsAtGreaterThanEqual(
                recurrenceId, expense.getCycle().getStartsAt()));
        recurringExpenseRepository.save(recurringExpense);
    }

    private ExpenseEntity findOwned(UUID walletId, UUID expenseId, UUID userId) {
        if (expenseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense id is required");
        }
        return expenseRepository
                .findByIdAndWalletIdAndWalletUserId(expenseId, walletId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private java.time.LocalDate requiredExpenseDate(ExpenseRequest request) {
        if (request.expenseDate() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense date is required");
        }
        return request.expenseDate();
    }
}
