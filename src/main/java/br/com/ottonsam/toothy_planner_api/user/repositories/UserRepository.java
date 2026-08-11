package br.com.ottonsam.toothy_planner_api.user.repositories;

import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserEntity> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);
}
