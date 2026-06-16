package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCycleEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseWalletEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.RecurringExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseCycleRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.RecurringExpenseRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class ExpenseCycleService {

    private final ExpenseCycleRepository cycleRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseRepository expenseRepository;

    public ExpenseCycleService(
            ExpenseCycleRepository cycleRepository,
            RecurringExpenseRepository recurringExpenseRepository,
            ExpenseRepository expenseRepository) {
        this.cycleRepository = cycleRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.expenseRepository = expenseRepository;
    }

    public ExpenseCycleEntity findOrCreateByDate(ExpenseWalletEntity wallet, LocalDate date) {
        var reference = referenceForDate(wallet, date);
        return cycleRepository
                .findByWalletIdAndReferenceMonthAndReferenceYear(wallet.getId(), reference.month(), reference.year())
                .orElseGet(() -> createCycle(wallet, reference));
    }

    public CycleReference referenceForDate(ExpenseWalletEntity wallet, LocalDate date) {
        var effectiveEndDay = effectiveEndDay(date.getYear(), date.getMonthValue(), wallet.getCycleEndDay());
        var referenceMonth = YearMonth.from(date);
        if (date.getDayOfMonth() > effectiveEndDay) {
            referenceMonth = referenceMonth.plusMonths(1);
        }
        return new CycleReference(referenceMonth.getMonthValue(), referenceMonth.getYear());
    }

    public CyclePeriod periodForReference(ExpenseWalletEntity wallet, CycleReference reference) {
        var referenceMonth = YearMonth.of(reference.year(), reference.month());
        var previousReferenceMonth = referenceMonth.minusMonths(1);
        var endsAt = endDateFor(referenceMonth, wallet.getCycleEndDay());
        var previousEndsAt = endDateFor(previousReferenceMonth, wallet.getCycleEndDay());
        return new CyclePeriod(previousEndsAt.plusDays(1), endsAt);
    }

    public int compare(ExpenseCycleEntity cycle, CycleReference reference) {
        var cycleYearMonth = YearMonth.of(cycle.getReferenceYear(), cycle.getReferenceMonth());
        var referenceYearMonth = YearMonth.of(reference.year(), reference.month());
        return cycleYearMonth.compareTo(referenceYearMonth);
    }

    public int compare(ExpenseCycleEntity left, ExpenseCycleEntity right) {
        var leftYearMonth = YearMonth.of(left.getReferenceYear(), left.getReferenceMonth());
        var rightYearMonth = YearMonth.of(right.getReferenceYear(), right.getReferenceMonth());
        return leftYearMonth.compareTo(rightYearMonth);
    }

    public LocalDate recurringExpenseDate(RecurringExpenseEntity recurringExpense, ExpenseCycleEntity cycle) {
        var startReference = referenceForDate(recurringExpense.getWallet(), recurringExpense.getStartsAt());
        var startPeriod = periodForReference(recurringExpense.getWallet(), startReference);
        var offset = ChronoUnit.DAYS.between(startPeriod.startsAt(), recurringExpense.getStartsAt());
        var expenseDate = cycle.getStartsAt().plusDays(offset);
        if (expenseDate.isAfter(cycle.getEndsAt())) {
            return cycle.getEndsAt();
        }
        return expenseDate;
    }

    private ExpenseCycleEntity createCycle(ExpenseWalletEntity wallet, CycleReference reference) {
        var period = periodForReference(wallet, reference);
        var cycle = cycleRepository.save(ExpenseCycleEntity.create(
                wallet, reference.month(), reference.year(), period.startsAt(), period.endsAt()));
        generateActiveRecurringExpenses(wallet, cycle);
        return cycle;
    }

    public void generateActiveRecurringExpenses(ExpenseWalletEntity wallet, ExpenseCycleEntity cycle) {
        recurringExpenseRepository.findAllByWalletIdAndCanceledAtIsNull(wallet.getId()).stream()
                .filter(recurringExpense -> shouldGenerate(recurringExpense, cycle))
                .filter(recurringExpense ->
                        !expenseRepository.existsByRecurrenceIdAndCycleId(recurringExpense.getId(), cycle.getId()))
                .map(recurringExpense -> ExpenseEntity.recurring(
                        wallet,
                        cycle,
                        recurringExpense.getCategory(),
                        recurringExpense.getDescription(),
                        recurringExpense.getAmount(),
                        recurringExpenseDate(recurringExpense, cycle),
                        recurringExpense.getId()))
                .forEach(expenseRepository::save);
    }

    private boolean shouldGenerate(RecurringExpenseEntity recurringExpense, ExpenseCycleEntity cycle) {
        var startReference = referenceForDate(recurringExpense.getWallet(), recurringExpense.getStartsAt());
        return compare(cycle, startReference) >= 0;
    }

    private LocalDate endDateFor(YearMonth referenceMonth, int cycleEndDay) {
        return referenceMonth.atDay(
                effectiveEndDay(referenceMonth.getYear(), referenceMonth.getMonthValue(), cycleEndDay));
    }

    private int effectiveEndDay(int year, int month, int cycleEndDay) {
        return Math.min(cycleEndDay, YearMonth.of(year, month).lengthOfMonth());
    }

    public record CycleReference(int month, int year) {}

    public record CyclePeriod(LocalDate startsAt, LocalDate endsAt) {}
}
