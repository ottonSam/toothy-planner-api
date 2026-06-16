package br.com.ottonsam.toothy_planner_api.financial_manager.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.financial_manager.dtos.ExpenseCycleResponse;
import br.com.ottonsam.toothy_planner_api.financial_manager.repositories.ExpenseCycleRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpenseCycleUseCase {

    private final ExpenseCycleRepository cycleRepository;
    private final ExpenseWalletUseCase walletUseCase;
    private final CurrentUserProvider currentUserProvider;

    public ExpenseCycleUseCase(
            ExpenseCycleRepository cycleRepository,
            ExpenseWalletUseCase walletUseCase,
            CurrentUserProvider currentUserProvider) {
        this.cycleRepository = cycleRepository;
        this.walletUseCase = walletUseCase;
        this.currentUserProvider = currentUserProvider;
    }

    public List<ExpenseCycleResponse> list(UUID walletId) {
        var user = currentUserProvider.get();
        walletUseCase.findOwned(walletId, user.getId());
        return cycleRepository
                .findAllByWalletIdAndWalletUserIdOrderByReferenceYearAscReferenceMonthAsc(walletId, user.getId())
                .stream()
                .map(ExpenseCycleResponse::from)
                .toList();
    }

    public ExpenseCycleResponse get(UUID walletId, UUID cycleId) {
        var user = currentUserProvider.get();
        walletUseCase.findOwned(walletId, user.getId());
        return ExpenseCycleResponse.from(cycleRepository
                .findByIdAndWalletIdAndWalletUserId(cycleId, walletId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cycle not found")));
    }
}
