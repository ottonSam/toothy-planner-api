package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.user.repositories.ProfileImageStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProfileImageUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final ProfileImageValidator profileImageValidator;
    private final ProfileImageStorage profileImageStorage;

    public UpdateProfileImageUseCase(
            CurrentUserProvider currentUserProvider,
            ProfileImageValidator profileImageValidator,
            ProfileImageStorage profileImageStorage) {
        this.currentUserProvider = currentUserProvider;
        this.profileImageValidator = profileImageValidator;
        this.profileImageStorage = profileImageStorage;
    }

    @Transactional
    public void execute(String base64Image) {
        var user = currentUserProvider.get();
        if (base64Image == null) {
            profileImageStorage.delete(user.getProfileImage());
            user.updateProfileImage(null);
            return;
        }

        var image = profileImageValidator.validate(base64Image);
        var oldImage = user.getProfileImage();
        var newImage = profileImageStorage.store(user.getId(), image);
        user.updateProfileImage(newImage);
        profileImageStorage.delete(oldImage);
    }
}
