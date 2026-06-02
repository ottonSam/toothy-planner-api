package br.com.ottonsam.toothy_planner_api.user.repositories;

import br.com.ottonsam.toothy_planner_api.user.entities.UserActivationCodeEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivationCodeRepository extends JpaRepository<UserActivationCodeEntity, UUID> {

    Optional<UserActivationCodeEntity> findFirstByUserIdAndCodeOrderByExpiresAtDesc(UUID userId, String code);
}
