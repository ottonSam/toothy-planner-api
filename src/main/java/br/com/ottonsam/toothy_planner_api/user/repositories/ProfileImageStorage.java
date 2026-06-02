package br.com.ottonsam.toothy_planner_api.user.repositories;

import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImageData;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImagePayload;
import java.util.Optional;
import java.util.UUID;

public interface ProfileImageStorage {

    String store(UUID userId, ProfileImagePayload image);

    Optional<ProfileImageData> load(String key);

    void delete(String key);
}
