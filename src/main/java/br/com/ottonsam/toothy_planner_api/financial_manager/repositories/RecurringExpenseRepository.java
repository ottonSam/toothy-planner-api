package br.com.ottonsam.toothy_planner_api.financial_manager.repositories;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.RecurringExpenseEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpenseEntity, UUID> {

    List<RecurringExpenseEntity> findAllByWalletIdAndWalletUserIdOrderByCreatedAtAsc(UUID walletId, UUID userId);

    List<RecurringExpenseEntity> findAllByWalletIdAndCanceledAtIsNull(UUID walletId);

    Optional<RecurringExpenseEntity> findByIdAndWalletIdAndWalletUserId(UUID id, UUID walletId, UUID userId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByWalletId(UUID walletId);
}
