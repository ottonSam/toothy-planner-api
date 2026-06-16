package br.com.ottonsam.toothy_planner_api.financial_manager.repositories;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseWalletEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseWalletRepository extends JpaRepository<ExpenseWalletEntity, UUID> {

    List<ExpenseWalletEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<ExpenseWalletEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByDescriptionIgnoreCaseAndUserId(String description, UUID userId);

    boolean existsByDescriptionIgnoreCaseAndUserIdAndIdNot(String description, UUID userId, UUID id);
}
