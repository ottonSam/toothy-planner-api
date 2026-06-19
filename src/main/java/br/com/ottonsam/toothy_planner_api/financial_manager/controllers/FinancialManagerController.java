package br.com.ottonsam.toothy_planner_api.financial_manager.controllers;

import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.CancelRecurringExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCategoryRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCategoryResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCycleMetricsResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCycleResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseWalletMetricsResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseWalletRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseWalletResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.InstallmentExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.InstallmentExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.RecurringExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.RecurringExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.ExpenseCategoryUseCase;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.ExpenseCycleUseCase;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.ExpenseMetricsUseCase;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.ExpenseUseCase;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.ExpenseWalletUseCase;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.InstallmentExpenseUseCase;
import br.com.ottonsam.toothy_planner_api.financial_manager.usecases.RecurringExpenseUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financial-manager")
public class FinancialManagerController {

    private final ExpenseCategoryUseCase categoryUseCase;
    private final ExpenseWalletUseCase walletUseCase;
    private final ExpenseCycleUseCase cycleUseCase;
    private final ExpenseMetricsUseCase metricsUseCase;
    private final ExpenseUseCase expenseUseCase;
    private final InstallmentExpenseUseCase installmentExpenseUseCase;
    private final RecurringExpenseUseCase recurringExpenseUseCase;

    public FinancialManagerController(
            ExpenseCategoryUseCase categoryUseCase,
            ExpenseWalletUseCase walletUseCase,
            ExpenseCycleUseCase cycleUseCase,
            ExpenseMetricsUseCase metricsUseCase,
            ExpenseUseCase expenseUseCase,
            InstallmentExpenseUseCase installmentExpenseUseCase,
            RecurringExpenseUseCase recurringExpenseUseCase) {
        this.categoryUseCase = categoryUseCase;
        this.walletUseCase = walletUseCase;
        this.cycleUseCase = cycleUseCase;
        this.metricsUseCase = metricsUseCase;
        this.expenseUseCase = expenseUseCase;
        this.installmentExpenseUseCase = installmentExpenseUseCase;
        this.recurringExpenseUseCase = recurringExpenseUseCase;
    }

    @PostMapping("/categories")
    ResponseEntity<ExpenseCategoryResponse> createCategory(@RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryUseCase.create(request));
    }

    @GetMapping("/categories")
    List<ExpenseCategoryResponse> listCategories() {
        return categoryUseCase.list();
    }

    @GetMapping("/categories/{categoryId}")
    ExpenseCategoryResponse getCategory(@PathVariable UUID categoryId) {
        return categoryUseCase.get(categoryId);
    }

    @PutMapping("/categories/{categoryId}")
    ExpenseCategoryResponse updateCategory(@PathVariable UUID categoryId, @RequestBody ExpenseCategoryRequest request) {
        return categoryUseCase.update(categoryId, request);
    }

    @DeleteMapping("/categories/{categoryId}")
    ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId) {
        categoryUseCase.delete(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/wallets")
    ResponseEntity<ExpenseWalletResponse> createWallet(@RequestBody ExpenseWalletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletUseCase.create(request));
    }

    @GetMapping("/wallets")
    List<ExpenseWalletResponse> listWallets() {
        return walletUseCase.list();
    }

    @GetMapping("/wallets/{walletId}")
    ExpenseWalletResponse getWallet(@PathVariable UUID walletId) {
        return walletUseCase.get(walletId);
    }

    @PutMapping("/wallets/{walletId}")
    ExpenseWalletResponse updateWallet(@PathVariable UUID walletId, @RequestBody ExpenseWalletRequest request) {
        return walletUseCase.update(walletId, request);
    }

    @DeleteMapping("/wallets/{walletId}")
    ResponseEntity<Void> deleteWallet(@PathVariable UUID walletId) {
        walletUseCase.delete(walletId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/wallets/{walletId}/metrics")
    ExpenseWalletMetricsResponse getWalletMetrics(@PathVariable UUID walletId) {
        return walletUseCase.metrics(walletId);
    }

    @GetMapping("/wallets/{walletId}/cycles")
    List<ExpenseCycleResponse> listCycles(@PathVariable UUID walletId) {
        return cycleUseCase.list(walletId);
    }

    @GetMapping("/wallets/{walletId}/cycles/{cycleId}")
    ExpenseCycleResponse getCycle(@PathVariable UUID walletId, @PathVariable UUID cycleId) {
        return cycleUseCase.get(walletId, cycleId);
    }

    @GetMapping("/wallets/{walletId}/cycles/{cycleId}/metrics")
    ExpenseCycleMetricsResponse getCycleMetrics(@PathVariable UUID walletId, @PathVariable UUID cycleId) {
        return metricsUseCase.getCycleMetrics(walletId, cycleId);
    }

    @GetMapping("/wallets/{walletId}/cycles/{cycleId}/expenses")
    List<ExpenseResponse> listCycleExpenses(@PathVariable UUID walletId, @PathVariable UUID cycleId) {
        return expenseUseCase.listByCycle(walletId, cycleId);
    }

    @PostMapping("/wallets/{walletId}/expenses")
    ResponseEntity<ExpenseResponse> createExpense(@PathVariable UUID walletId, @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseUseCase.create(walletId, request));
    }

    @GetMapping("/wallets/{walletId}/expenses")
    List<ExpenseResponse> listExpenses(@PathVariable UUID walletId) {
        return expenseUseCase.list(walletId);
    }

    @GetMapping("/wallets/{walletId}/expenses/{expenseId}")
    ExpenseResponse getExpense(@PathVariable UUID walletId, @PathVariable UUID expenseId) {
        return expenseUseCase.get(walletId, expenseId);
    }

    @PutMapping("/wallets/{walletId}/expenses/{expenseId}")
    ExpenseResponse updateExpense(
            @PathVariable UUID walletId, @PathVariable UUID expenseId, @RequestBody ExpenseRequest request) {
        return expenseUseCase.update(walletId, expenseId, request);
    }

    @DeleteMapping("/wallets/{walletId}/expenses/{expenseId}")
    ResponseEntity<Void> deleteExpense(@PathVariable UUID walletId, @PathVariable UUID expenseId) {
        expenseUseCase.delete(walletId, expenseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/wallets/{walletId}/installment-expenses")
    ResponseEntity<InstallmentExpenseResponse> createInstallmentExpense(
            @PathVariable UUID walletId, @RequestBody InstallmentExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(installmentExpenseUseCase.create(walletId, request));
    }

    @GetMapping("/wallets/{walletId}/installment-expenses")
    List<InstallmentExpenseResponse> listInstallmentExpenses(@PathVariable UUID walletId) {
        return installmentExpenseUseCase.list(walletId);
    }

    @GetMapping("/wallets/{walletId}/installment-expenses/{installmentExpenseId}")
    InstallmentExpenseResponse getInstallmentExpense(
            @PathVariable UUID walletId, @PathVariable UUID installmentExpenseId) {
        return installmentExpenseUseCase.get(walletId, installmentExpenseId);
    }

    @PutMapping("/wallets/{walletId}/installment-expenses/{installmentExpenseId}")
    InstallmentExpenseResponse updateInstallmentExpense(
            @PathVariable UUID walletId,
            @PathVariable UUID installmentExpenseId,
            @RequestBody InstallmentExpenseRequest request) {
        return installmentExpenseUseCase.update(walletId, installmentExpenseId, request);
    }

    @DeleteMapping("/wallets/{walletId}/installment-expenses/{installmentExpenseId}")
    ResponseEntity<Void> deleteInstallmentExpense(
            @PathVariable UUID walletId, @PathVariable UUID installmentExpenseId) {
        installmentExpenseUseCase.delete(walletId, installmentExpenseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/wallets/{walletId}/recurring-expenses")
    ResponseEntity<RecurringExpenseResponse> createRecurringExpense(
            @PathVariable UUID walletId, @RequestBody RecurringExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringExpenseUseCase.create(walletId, request));
    }

    @GetMapping("/wallets/{walletId}/recurring-expenses")
    List<RecurringExpenseResponse> listRecurringExpenses(@PathVariable UUID walletId) {
        return recurringExpenseUseCase.list(walletId);
    }

    @GetMapping("/wallets/{walletId}/recurring-expenses/{recurringExpenseId}")
    RecurringExpenseResponse getRecurringExpense(@PathVariable UUID walletId, @PathVariable UUID recurringExpenseId) {
        return recurringExpenseUseCase.get(walletId, recurringExpenseId);
    }

    @PutMapping("/wallets/{walletId}/recurring-expenses/{recurringExpenseId}")
    RecurringExpenseResponse updateRecurringExpense(
            @PathVariable UUID walletId,
            @PathVariable UUID recurringExpenseId,
            @RequestBody RecurringExpenseRequest request) {
        return recurringExpenseUseCase.update(walletId, recurringExpenseId, request);
    }

    @PostMapping("/wallets/{walletId}/recurring-expenses/{recurringExpenseId}/cancel")
    RecurringExpenseResponse cancelRecurringExpense(
            @PathVariable UUID walletId,
            @PathVariable UUID recurringExpenseId,
            @RequestBody CancelRecurringExpenseRequest request) {
        return recurringExpenseUseCase.cancel(walletId, recurringExpenseId, request);
    }
}
