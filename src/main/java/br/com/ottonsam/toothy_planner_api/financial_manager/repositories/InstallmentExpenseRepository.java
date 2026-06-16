package br.com.ottonsam.toothy_planner_api.financial_manager.repositories;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.InstallmentExpenseEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentExpenseRepository extends JpaRepository<InstallmentExpenseEntity, UUID> {

    List<InstallmentExpenseEntity> findAllByWalletIdAndWalletUserIdOrderByCreatedAtAsc(UUID walletId, UUID userId);

    Optional<InstallmentExpenseEntity> findByIdAndWalletIdAndWalletUserId(UUID id, UUID walletId, UUID userId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByWalletId(UUID walletId);
}
