package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.CancelRecurringExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.RecurringExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.RecurringExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.RecurringExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseCycleRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseRepository;
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
public class RecurringExpenseUseCase {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseCycleRepository cycleRepository;
    private final ExpenseWalletUseCase walletUseCase;
    private final ExpenseCategoryUseCase categoryUseCase;
    private final ExpenseCycleService cycleService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public RecurringExpenseUseCase(
            RecurringExpenseRepository recurringExpenseRepository,
            ExpenseRepository expenseRepository,
            ExpenseCycleRepository cycleRepository,
            ExpenseWalletUseCase walletUseCase,
            ExpenseCategoryUseCase categoryUseCase,
            ExpenseCycleService cycleService,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.expenseRepository = expenseRepository;
        this.cycleRepository = cycleRepository;
        this.walletUseCase = walletUseCase;
        this.categoryUseCase = categoryUseCase;
        this.cycleService = cycleService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public RecurringExpenseResponse create(UUID walletId, RecurringExpenseRequest request) {
        var user = currentUserProvider.get();
        var wallet = walletUseCase.findOwned(walletId, user.getId());
        var category = categoryUseCase.findOwned(request.categoryId(), user.getId());
        var recurringExpense = recurringExpenseRepository.save(RecurringExpenseEntity.create(
                wallet, category, request.description(), request.amount(), request.startsAt()));
        cycleService.findOrCreateByDate(wallet, request.startsAt());
        generateForExistingCycles(recurringExpense, null);
        return RecurringExpenseResponse.from(recurringExpense);
    }

    public List<RecurringExpenseResponse> list(UUID walletId) {
        var user = currentUserProvider.get();
        walletUseCase.findOwned(walletId, user.getId());
        return recurringExpenseRepository
                .findAllByWalletIdAndWalletUserIdOrderByCreatedAtAsc(walletId, user.getId())
                .stream()
                .map(RecurringExpenseResponse::from)
                .toList();
    }

    public RecurringExpenseResponse get(UUID walletId, UUID recurringExpenseId) {
        var user = currentUserProvider.get();
        return RecurringExpenseResponse.from(findOwned(walletId, recurringExpenseId, user.getId()));
    }

    public RecurringExpenseResponse update(UUID walletId, UUID recurringExpenseId, RecurringExpenseRequest request) {
        var user = currentUserProvider.get();
        var recurringExpense = findOwned(walletId, recurringExpenseId, user.getId());
        if (!recurringExpense.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recurring expense is canceled");
        }
        var category = categoryUseCase.findOwned(request.categoryId(), user.getId());
        recurringExpense.update(category, request.description(), request.amount(), request.startsAt());
        var today = LocalDate.now(clock);
        expenseRepository.deleteAll(
                expenseRepository.findAllByRecurrenceIdAndCycle_EndsAtGreaterThanEqual(recurringExpenseId, today));
        cycleService.findOrCreateByDate(recurringExpense.getWallet(), recurringExpense.getStartsAt());
        generateForExistingCycles(recurringExpense, today);
        return RecurringExpenseResponse.from(recurringExpenseRepository.save(recurringExpense));
    }

    public RecurringExpenseResponse cancel(
            UUID walletId, UUID recurringExpenseId, CancelRecurringExpenseRequest request) {
        var user = currentUserProvider.get();
        var recurringExpense = findOwned(walletId, recurringExpenseId, user.getId());
        if (request.cycleId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cycle id is required");
        }
        var cycle = cycleRepository
                .findByIdAndWalletIdAndWalletUserId(request.cycleId(), walletId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cycle not found"));
        recurringExpense.cancel(cycle.getStartsAt());
        expenseRepository.deleteAll(
                expenseRepository.findAllByRecurrenceIdAndCycle_StartsAtAfter(recurringExpenseId, cycle.getStartsAt()));
        return RecurringExpenseResponse.from(recurringExpenseRepository.save(recurringExpense));
    }

    private RecurringExpenseEntity findOwned(UUID walletId, UUID recurringExpenseId, UUID userId) {
        if (recurringExpenseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Recurring expense id is required");
        }
        return recurringExpenseRepository
                .findByIdAndWalletIdAndWalletUserId(recurringExpenseId, walletId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recurring expense not found"));
    }

    private void generateForExistingCycles(
            RecurringExpenseEntity recurringExpense, LocalDate onlyCyclesEndingOnOrAfter) {
        var startReference =
                cycleService.referenceForDate(recurringExpense.getWallet(), recurringExpense.getStartsAt());
        cycleRepository
                .findAllByWalletIdAndWalletUserIdOrderByReferenceYearAscReferenceMonthAsc(
                        recurringExpense.getWallet().getId(),
                        recurringExpense.getWallet().getUser().getId())
                .stream()
                .filter(cycle -> cycleService.compare(cycle, startReference) >= 0)
                .filter(cycle ->
                        onlyCyclesEndingOnOrAfter == null || !cycle.getEndsAt().isBefore(onlyCyclesEndingOnOrAfter))
                .filter(cycle ->
                        !expenseRepository.existsByRecurrenceIdAndCycleId(recurringExpense.getId(), cycle.getId()))
                .map(cycle -> ExpenseEntity.recurring(
                        recurringExpense.getWallet(),
                        cycle,
                        recurringExpense.getCategory(),
                        recurringExpense.getDescription(),
                        recurringExpense.getAmount(),
                        cycleService.recurringExpenseDate(recurringExpense, cycle),
                        recurringExpense.getId()))
                .forEach(expenseRepository::save);
    }
}
