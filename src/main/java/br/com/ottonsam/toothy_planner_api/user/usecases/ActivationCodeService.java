package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.user.entities.UserActivationCodeEntity;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import br.com.ottonsam.toothy_planner_api.user.repositories.UserActivationCodeRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class ActivationCodeService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserActivationCodeRepository activationCodeRepository;

    public ActivationCodeService(UserActivationCodeRepository activationCodeRepository) {
        this.activationCodeRepository = activationCodeRepository;
    }

    public String createFor(UserEntity user) {
        var code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        activationCodeRepository.save(UserActivationCodeEntity.create(
                user.getId(), code, OffsetDateTime.now().plusMinutes(15)));
        System.out.printf("Activation code for %s: %s%n", user.getEmail(), code);
        return code;
    }
}
