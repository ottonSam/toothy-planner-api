package br.com.ottonsam.toothy_planner_api.financial_manager.repositories;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseCategoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategoryEntity, UUID> {

    List<ExpenseCategoryEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<ExpenseCategoryEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByNameIgnoreCaseAndUserId(String name, UUID userId);

    boolean existsByNameIgnoreCaseAndUserIdAndIdNot(String name, UUID userId, UUID id);
}
