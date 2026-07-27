package br.com.ottonsam.toothy_planner_api.user.usecases;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.entities.UserEntity;
import br.com.ottonsam.toothy_planner_api.user.repositories.ProfileImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UpdateProfileImageUseCaseTests {

    @Test
    void preservesCurrentImageWhenNewImageCannotBeStored() {
        var currentUserProvider = mock(CurrentUserProvider.class);
        var profileImageValidator = mock(ProfileImageValidator.class);
        var profileImageStorage = mock(ProfileImageStorage.class);
        var user = mock(UserEntity.class);
        var image = new ProfileImagePayload(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}, "image/png", "png");
        var failure = new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store profile image");
        when(currentUserProvider.get()).thenReturn(user);
        when(user.getProfileImage()).thenReturn("users/user-id/profile-image/current.png");
        when(profileImageValidator.validate("base64-image")).thenReturn(image);
        when(profileImageStorage.store(user.getId(), image)).thenThrow(failure);
        var useCase = new UpdateProfileImageUseCase(currentUserProvider, profileImageValidator, profileImageStorage);

        assertThatThrownBy(() -> useCase.execute("base64-image")).isSameAs(failure);

        verify(user, never()).updateProfileImage(org.mockito.ArgumentMatchers.any());
        verify(profileImageStorage, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
