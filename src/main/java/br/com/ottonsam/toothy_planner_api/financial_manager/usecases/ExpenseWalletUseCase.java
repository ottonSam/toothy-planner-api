package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseWalletMetricsResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseWalletRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseWalletResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseWalletEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseCycleRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseWalletRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.InstallmentExpenseRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.RecurringExpenseRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseWalletUseCase {

    private final ExpenseWalletRepository walletRepository;
    private final ExpenseCycleRepository cycleRepository;
    private final ExpenseRepository expenseRepository;
    private final InstallmentExpenseRepository installmentExpenseRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseCycleService cycleService;
    private final ExpenseMetricsUseCase metricsUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public ExpenseWalletUseCase(
            ExpenseWalletRepository walletRepository,
            ExpenseCycleRepository cycleRepository,
            ExpenseRepository expenseRepository,
            InstallmentExpenseRepository installmentExpenseRepository,
            RecurringExpenseRepository recurringExpenseRepository,
            ExpenseCycleService cycleService,
            ExpenseMetricsUseCase metricsUseCase,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.walletRepository = walletRepository;
        this.cycleRepository = cycleRepository;
        this.expenseRepository = expenseRepository;
        this.installmentExpenseRepository = installmentExpenseRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.cycleService = cycleService;
        this.metricsUseCase = metricsUseCase;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public ExpenseWalletResponse create(ExpenseWalletRequest request) {
        var user = currentUserProvider.get();
        ensureDescriptionIsUnique(request.description(), user.getId());
        var wallet =
                ExpenseWalletEntity.create(request.description(), request.spendingGoal(), request.cycleEndDay(), user);
        return ExpenseWalletResponse.from(walletRepository.save(wallet));
    }

    public List<ExpenseWalletResponse> list() {
        var user = currentUserProvider.get();
        return walletRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(ExpenseWalletResponse::from)
                .toList();
    }

    public ExpenseWalletResponse get(UUID id) {
        var user = currentUserProvider.get();
        return ExpenseWalletResponse.from(findOwned(id, user.getId()));
    }

    public ExpenseWalletResponse update(UUID id, ExpenseWalletRequest request) {
        var user = currentUserProvider.get();
        var wallet = findOwned(id, user.getId());
        ensureDescriptionIsUniqueForUpdate(request.description(), user.getId(), id);
        wallet.update(request.description(), request.spendingGoal(), request.cycleEndDay());
        return ExpenseWalletResponse.from(walletRepository.save(wallet));
    }

    public void delete(UUID id) {
        var user = currentUserProvider.get();
        var wallet = findOwned(id, user.getId());
        if (expenseRepository.existsByWalletId(id)
                || installmentExpenseRepository.existsByWalletId(id)
                || recurringExpenseRepository.existsByWalletId(id)
                || !cycleRepository
                        .findAllByWalletIdAndWalletUserIdOrderByReferenceYearAscReferenceMonthAsc(id, user.getId())
                        .isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "Wallet is associated with cycles or expenses");
        }
        walletRepository.delete(wallet);
    }

    public ExpenseWalletMetricsResponse metrics(UUID id) {
        var user = currentUserProvider.get();
        var wallet = findOwned(id, user.getId());
        var today = LocalDate.now(clock);
        var currentReference = cycleService.referenceForDate(wallet, today);
        var currentCycle = cycleRepository
                .findByWalletIdAndReferenceMonthAndReferenceYear(
                        wallet.getId(), currentReference.month(), currentReference.year())
                .orElse(null);
        var currentCycleMetrics =
                currentCycle == null ? null : metricsUseCase.calculateCycleMetrics(wallet, currentCycle);
        return new ExpenseWalletMetricsResponse(
                wallet.getId(),
                wallet.getDescription(),
                wallet.getSpendingGoal(),
                wallet.getCycleEndDay(),
                currentCycle == null
                        ? null
                        : br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCycleResponse.from(
                                currentCycle),
                currentCycleMetrics,
                metricsUseCase.activeRecurringMonthlyTotal(wallet.getId()),
                metricsUseCase.installmentTotalFromReference(wallet.getId(), currentReference));
    }

    ExpenseWalletEntity findOwned(UUID id, UUID userId) {
        if (id == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Wallet id is required");
        }
        return walletRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    private void ensureDescriptionIsUnique(String description, UUID userId) {
        ExpenseWalletEntity.validateDescription(description);
        if (walletRepository.existsByDescriptionIgnoreCaseAndUserId(description.trim(), userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Wallet description already exists");
        }
    }

    private void ensureDescriptionIsUniqueForUpdate(String description, UUID userId, UUID id) {
        ExpenseWalletEntity.validateDescription(description);
        if (walletRepository.existsByDescriptionIgnoreCaseAndUserIdAndIdNot(description.trim(), userId, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Wallet description already exists");
        }
    }
}
