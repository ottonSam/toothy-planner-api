package br.com.ottonsam.toothy_planner_api.diet.repositories;

import br.com.ottonsam.toothy_planner_api.diet.entities.FoodEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<FoodEntity, UUID> {

    List<FoodEntity> findAllByUserIdOrderByNameAsc(UUID userId);

    List<FoodEntity> findAllByUserIdAndNameContainingOrderByNameAsc(UUID userId, String name);

    Optional<FoodEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<FoodEntity> findByNameAndUserId(String name, UUID userId);
}
