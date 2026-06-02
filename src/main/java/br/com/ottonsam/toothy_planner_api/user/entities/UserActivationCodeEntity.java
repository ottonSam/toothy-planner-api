package br.com.ottonsam.toothy_planner_api.user.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_activation_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActivationCodeEntity {

    @Id
    @NotNull(message = "Activation code id is required") private UUID id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User id is required") private UUID userId;

    @Column(nullable = false)
    @NotBlank(message = "Activation code is required") private String code;

    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Activation code expiration is required") private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    private UserActivationCodeEntity(UUID userId, String code, OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public static UserActivationCodeEntity create(UUID userId, String code, OffsetDateTime expiresAt) {
        return new UserActivationCodeEntity(userId, code, expiresAt);
    }

    public boolean isValid(OffsetDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed() {
        this.usedAt = OffsetDateTime.now();
    }
}
