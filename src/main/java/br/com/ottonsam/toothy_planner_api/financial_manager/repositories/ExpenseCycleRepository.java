package br.com.ottonsam.toothy_planner_api.financial_manager.repositories;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCycleEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCycleRepository extends JpaRepository<ExpenseCycleEntity, UUID> {

    List<ExpenseCycleEntity> findAllByWalletIdAndWalletUserIdOrderByReferenceYearAscReferenceMonthAsc(
            UUID walletId, UUID userId);

    Optional<ExpenseCycleEntity> findByIdAndWalletIdAndWalletUserId(UUID id, UUID walletId, UUID userId);

    Optional<ExpenseCycleEntity> findByWalletIdAndReferenceMonthAndReferenceYear(
            UUID walletId, int referenceMonth, int referenceYear);
}
