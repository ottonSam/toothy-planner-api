package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.user.repositories.ProfileImageStorage;
import org.springframework.stereotype.Service;

@Service
public class GetProfileImageUseCase {

    private static final byte[] DEFAULT_IMAGE = """
            <svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">
              <rect width="256" height="256" fill="#f3f4f6"/>
              <circle cx="128" cy="96" r="44" fill="#9ca3af"/>
              <path d="M48 224c10-45 40-72 80-72s70 27 80 72" fill="#9ca3af"/>
            </svg>
            """.getBytes(java.nio.charset.StandardCharsets.UTF_8);

    private final CurrentUserProvider currentUserProvider;
    private final ProfileImageStorage profileImageStorage;

    public GetProfileImageUseCase(CurrentUserProvider currentUserProvider, ProfileImageStorage profileImageStorage) {
        this.currentUserProvider = currentUserProvider;
        this.profileImageStorage = profileImageStorage;
    }

    public ProfileImageData execute() {
        var user = currentUserProvider.get();
        if (user.getProfileImage() == null || user.getProfileImage().isBlank()) {
            return new ProfileImageData(DEFAULT_IMAGE, "image/svg+xml");
        }
        return profileImageStorage
                .load(user.getProfileImage())
                .orElse(new ProfileImageData(DEFAULT_IMAGE, "image/svg+xml"));
    }
}
