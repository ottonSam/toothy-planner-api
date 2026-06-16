package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCycleMetricsResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCycleEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseType;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseWalletEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseCycleRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseWalletRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.RecurringExpenseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpenseMetricsUseCase {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ExpenseRepository expenseRepository;
    private final ExpenseCycleRepository cycleRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseWalletRepository walletRepository;
    private final ExpenseCycleService cycleService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public ExpenseMetricsUseCase(
            ExpenseRepository expenseRepository,
            ExpenseCycleRepository cycleRepository,
            RecurringExpenseRepository recurringExpenseRepository,
            ExpenseWalletRepository walletRepository,
            ExpenseCycleService cycleService,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.expenseRepository = expenseRepository;
        this.cycleRepository = cycleRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.walletRepository = walletRepository;
        this.cycleService = cycleService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public ExpenseCycleMetricsResponse getCycleMetrics(UUID walletId, UUID cycleId) {
        var user = currentUserProvider.get();
        var wallet = walletRepository
                .findByIdAndUserId(walletId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        var cycle = cycleRepository
                .findByIdAndWalletIdAndWalletUserId(cycleId, walletId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cycle not found"));
        return calculateCycleMetrics(wallet, cycle);
    }

    public ExpenseCycleMetricsResponse calculateCycleMetrics(ExpenseWalletEntity wallet, ExpenseCycleEntity cycle) {
        var expenses = expenseRepository.findAllByCycleId(cycle.getId());
        var totalSpent = sum(expenses.stream().map(ExpenseEntity::getAmount));
        var oneTimeTotal = sum(expenses.stream()
                .filter(expense -> expense.getType() == ExpenseType.ONE_TIME)
                .map(ExpenseEntity::getAmount));
        var remainingAmount = money(wallet.getSpendingGoal().subtract(totalSpent));
        var remainingDailyAmount = remainingDailyAmount(cycle, remainingAmount);
        var reference = new ExpenseCycleService.CycleReference(cycle.getReferenceMonth(), cycle.getReferenceYear());
        return new ExpenseCycleMetricsResponse(
                wallet.getId(),
                cycle.getId(),
                cycle.getReferenceMonth(),
                cycle.getReferenceYear(),
                cycle.getStartsAt(),
                cycle.getEndsAt(),
                money(wallet.getSpendingGoal()),
                totalSpent,
                remainingAmount,
                remainingDailyAmount,
                installmentTotalFromReference(wallet.getId(), reference),
                activeRecurringMonthlyTotal(wallet.getId()),
                oneTimeTotal);
    }

    BigDecimal activeRecurringMonthlyTotal(UUID walletId) {
        return sum(recurringExpenseRepository.findAllByWalletIdAndCanceledAtIsNull(walletId).stream()
                .map(recurringExpense -> recurringExpense.getAmount()));
    }

    BigDecimal installmentTotalFromReference(UUID walletId, ExpenseCycleService.CycleReference reference) {
        return sum(expenseRepository.findAllByWalletIdAndType(walletId, ExpenseType.INSTALLMENT).stream()
                .filter(expense -> cycleService.compare(expense.getCycle(), reference) >= 0)
                .map(ExpenseEntity::getAmount));
    }

    private BigDecimal remainingDailyAmount(ExpenseCycleEntity cycle, BigDecimal remainingAmount) {
        var today = LocalDate.now(clock);
        if (today.isAfter(cycle.getEndsAt())) {
            return ZERO;
        }
        var start = today.isBefore(cycle.getStartsAt()) ? cycle.getStartsAt() : today;
        var days = ChronoUnit.DAYS.between(start, cycle.getEndsAt()) + 1;
        if (days <= 0) {
            return ZERO;
        }
        return remainingAmount.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(java.util.stream.Stream<BigDecimal> values) {
        return money(values.reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
