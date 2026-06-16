package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.InstallmentExpenseRequest;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.InstallmentExpenseResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.InstallmentExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseRepository;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.InstallmentExpenseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InstallmentExpenseUseCase {

    private final InstallmentExpenseRepository installmentExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseWalletUseCase walletUseCase;
    private final ExpenseCategoryUseCase categoryUseCase;
    private final ExpenseCycleService cycleService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public InstallmentExpenseUseCase(
            InstallmentExpenseRepository installmentExpenseRepository,
            ExpenseRepository expenseRepository,
            ExpenseWalletUseCase walletUseCase,
            ExpenseCategoryUseCase categoryUseCase,
            ExpenseCycleService cycleService,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.installmentExpenseRepository = installmentExpenseRepository;
        this.expenseRepository = expenseRepository;
        this.walletUseCase = walletUseCase;
        this.categoryUseCase = categoryUseCase;
        this.cycleService = cycleService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public InstallmentExpenseResponse create(UUID walletId, InstallmentExpenseRequest request) {
        var user = currentUserProvider.get();
        var wallet = walletUseCase.findOwned(walletId, user.getId());
        var category = categoryUseCase.findOwned(request.categoryId(), user.getId());
        var installmentExpense = installmentExpenseRepository.save(InstallmentExpenseEntity.create(
                wallet,
                category,
                request.description(),
                request.totalAmount(),
                request.installmentAmount(),
                request.installments(),
                request.firstExpenseDate()));
        generateInstallments(installmentExpense, null);
        return InstallmentExpenseResponse.from(installmentExpense);
    }

    public List<InstallmentExpenseResponse> list(UUID walletId) {
        var user = currentUserProvider.get();
        walletUseCase.findOwned(walletId, user.getId());
        return installmentExpenseRepository
                .findAllByWalletIdAndWalletUserIdOrderByCreatedAtAsc(walletId, user.getId())
                .stream()
                .map(InstallmentExpenseResponse::from)
                .toList();
    }

    public InstallmentExpenseResponse get(UUID walletId, UUID installmentExpenseId) {
        var user = currentUserProvider.get();
        return InstallmentExpenseResponse.from(findOwned(walletId, installmentExpenseId, user.getId()));
    }

    public InstallmentExpenseResponse update(
            UUID walletId, UUID installmentExpenseId, InstallmentExpenseRequest request) {
        var user = currentUserProvider.get();
        var installmentExpense = findOwned(walletId, installmentExpenseId, user.getId());
        var category = categoryUseCase.findOwned(request.categoryId(), user.getId());
        installmentExpense.update(
                category,
                request.description(),
                request.totalAmount(),
                request.installmentAmount(),
                request.installments(),
                request.firstExpenseDate());
        var today = LocalDate.now(clock);
        expenseRepository.deleteAll(
                expenseRepository.findAllByParentExpenseIdAndCycle_EndsAtGreaterThanEqual(installmentExpenseId, today));
        generateInstallments(installmentExpense, today);
        return InstallmentExpenseResponse.from(installmentExpenseRepository.save(installmentExpense));
    }

    public void delete(UUID walletId, UUID installmentExpenseId) {
        var user = currentUserProvider.get();
        var installmentExpense = findOwned(walletId, installmentExpenseId, user.getId());
        expenseRepository.deleteAll(expenseRepository.findAllByParentExpenseId(installmentExpenseId));
        installmentExpenseRepository.delete(installmentExpense);
    }

    private InstallmentExpenseEntity findOwned(UUID walletId, UUID installmentExpenseId, UUID userId) {
        if (installmentExpenseId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Installment expense id is required");
        }
        return installmentExpenseRepository
                .findByIdAndWalletIdAndWalletUserId(installmentExpenseId, walletId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Installment expense not found"));
    }

    private void generateInstallments(
            InstallmentExpenseEntity installmentExpense, LocalDate onlyCyclesEndingOnOrAfter) {
        var amount = installmentAmount(installmentExpense);
        for (int index = 0; index < installmentExpense.getInstallments(); index++) {
            var expenseDate = installmentExpense.getFirstExpenseDate().plusMonths(index);
            var cycle = cycleService.findOrCreateByDate(installmentExpense.getWallet(), expenseDate);
            if (onlyCyclesEndingOnOrAfter != null && cycle.getEndsAt().isBefore(onlyCyclesEndingOnOrAfter)) {
                continue;
            }
            expenseRepository.save(ExpenseEntity.installment(
                    installmentExpense.getWallet(),
                    cycle,
                    installmentExpense.getCategory(),
                    installmentExpense.getDescription(),
                    amount,
                    expenseDate,
                    installmentExpense.getId(),
                    index + 1,
                    installmentExpense.getInstallments()));
        }
    }

    private BigDecimal installmentAmount(InstallmentExpenseEntity installmentExpense) {
        if (installmentExpense.getInstallmentAmount() != null) {
            return installmentExpense.getInstallmentAmount().setScale(2, RoundingMode.HALF_UP);
        }
        return installmentExpense
                .getTotalAmount()
                .divide(BigDecimal.valueOf(installmentExpense.getInstallments()), 2, RoundingMode.CEILING);
    }
}
