package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCycleEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseWalletEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.RecurringExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseCycleRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.RecurringExpenseRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.http.HttpStatus;
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
        validateWalletAndDate(wallet, date);
        var period = periodForDate(wallet, date);
        return referenceForPeriod(period);
    }

    public CyclePeriod periodForReference(ExpenseWalletEntity wallet, CycleReference reference) {
        validateWallet(wallet);
        var endMonth = YearMonth.of(reference.year(), reference.month());
        var start = anchoredDate(wallet.getStartsAt(), endMonth.minusMonths(1));
        var end = anchoredDate(wallet.getStartsAt(), endMonth).minusDays(1);
        if (end.isBefore(wallet.getStartsAt())) {
            start = wallet.getStartsAt();
            end = anchoredDate(
                            wallet.getStartsAt(),
                            YearMonth.from(wallet.getStartsAt()).plusMonths(1))
                    .minusDays(1);
        }
        return new CyclePeriod(start, end, targetSpendingDate(start, end, wallet.getTargetSpendingDay()));
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
        return dateForDayInCycle(cycle, recurringExpense.getStartsAt().getDayOfMonth());
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

    private ExpenseCycleEntity createCycle(ExpenseWalletEntity wallet, CycleReference reference) {
        var period = periodForReference(wallet, reference);
        var cycle = cycleRepository.save(ExpenseCycleEntity.create(
                wallet,
                reference.month(),
                reference.year(),
                period.startsAt(),
                period.endsAt(),
                period.targetSpendingDate()));
        generateActiveRecurringExpenses(wallet, cycle);
        return cycle;
    }

    private boolean shouldGenerate(RecurringExpenseEntity recurringExpense, ExpenseCycleEntity cycle) {
        var startReference = referenceForDate(recurringExpense.getWallet(), recurringExpense.getStartsAt());
        return compare(cycle, startReference) >= 0;
    }

    private CyclePeriod periodForDate(ExpenseWalletEntity wallet, LocalDate date) {
        var start = anchoredDate(wallet.getStartsAt(), YearMonth.from(date));
        if (start.isAfter(date)) {
            start = anchoredDate(wallet.getStartsAt(), YearMonth.from(date).minusMonths(1));
        }
        if (start.isBefore(wallet.getStartsAt())) {
            start = wallet.getStartsAt();
        }
        var end = anchoredDate(wallet.getStartsAt(), YearMonth.from(start).plusMonths(1))
                .minusDays(1);
        return new CyclePeriod(start, end, targetSpendingDate(start, end, wallet.getTargetSpendingDay()));
    }

    private CycleReference referenceForPeriod(CyclePeriod period) {
        return new CycleReference(
                period.endsAt().getMonthValue(), period.endsAt().getYear());
    }

    private LocalDate anchoredDate(LocalDate walletStartsAt, YearMonth month) {
        return month.atDay(Math.min(walletStartsAt.getDayOfMonth(), month.lengthOfMonth()));
    }

    private LocalDate targetSpendingDate(LocalDate startsAt, LocalDate endsAt, int targetSpendingDay) {
        var target = dateForDayBetween(startsAt, endsAt, targetSpendingDay);
        if (target == null) {
            return endsAt;
        }
        return target;
    }

    private LocalDate dateForDayInCycle(ExpenseCycleEntity cycle, int dayOfMonth) {
        var target = dateForDayBetween(cycle.getStartsAt(), cycle.getEndsAt(), dayOfMonth);
        if (target == null) {
            return cycle.getEndsAt();
        }
        return target;
    }

    private LocalDate dateForDayBetween(LocalDate startsAt, LocalDate endsAt, int dayOfMonth) {
        var month = YearMonth.from(startsAt);
        while (!month.atDay(1).isAfter(endsAt)) {
            var date = month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
            if (!date.isBefore(startsAt) && !date.isAfter(endsAt)) {
                return date;
            }
            month = month.plusMonths(1);
        }
        return null;
    }

    private void validateWalletAndDate(ExpenseWalletEntity wallet, LocalDate date) {
        validateWallet(wallet);
        if (date == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense date is required");
        }
        if (date.isBefore(wallet.getStartsAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Expense date must be on or after wallet start date");
        }
    }

    private void validateWallet(ExpenseWalletEntity wallet) {
        if (wallet == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Wallet is required");
        }
    }

    public record CycleReference(int month, int year) {}

    public record CyclePeriod(LocalDate startsAt, LocalDate endsAt, LocalDate targetSpendingDate) {}
}
