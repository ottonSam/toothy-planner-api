package br.com.ottonsam.toothy_planner_api.financial_manager.repositories;

import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseEntity;
import br.com.ottonsam.toothy_planner_api.financial_manager.entities.ExpenseType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, UUID> {

    List<ExpenseEntity> findAllByWalletIdAndWalletUserIdOrderByExpenseDateAscCreatedAtAsc(UUID walletId, UUID userId);

    List<ExpenseEntity> findAllByWalletIdOrderByExpenseDateAscCreatedAtAsc(UUID walletId);

    List<ExpenseEntity> findAllByCycleId(UUID cycleId);

    List<ExpenseEntity> findAllByCycleIdAndWalletIdAndWalletUserIdOrderByExpenseDateAscCreatedAtAsc(
            UUID cycleId, UUID walletId, UUID userId);

    List<ExpenseEntity> findAllByParentExpenseId(UUID parentExpenseId);

    List<ExpenseEntity> findAllByParentExpenseIdAndCycle_EndsAtGreaterThanEqual(
            UUID parentExpenseId, LocalDate cycleEndDate);

    List<ExpenseEntity> findAllByRecurrenceId(UUID recurrenceId);

    List<ExpenseEntity> findAllByRecurrenceIdAndCycle_StartsAtAfter(UUID recurrenceId, LocalDate cycleStartDate);

    List<ExpenseEntity> findAllByRecurrenceIdAndCycle_EndsAtGreaterThanEqual(UUID recurrenceId, LocalDate cycleEndDate);

    Optional<ExpenseEntity> findByIdAndWalletIdAndWalletUserId(UUID id, UUID walletId, UUID userId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByWalletId(UUID walletId);

    boolean existsByRecurrenceIdAndCycleId(UUID recurrenceId, UUID cycleId);

    List<ExpenseEntity> findAllByWalletIdAndType(UUID walletId, ExpenseType type);
}
