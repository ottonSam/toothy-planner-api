package br.com.ottonsam.toothy_planner_api.user.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImagePayload;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MinioProfileImageStorageTests {

    private static final String BUCKET = "profile-images";
    private static final ProfileImagePayload IMAGE =
            new ProfileImagePayload(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}, "image/png", "png");

    @Test
    void storesImageInExistingBucket() throws Exception {
        var minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));
        var storage = new MinioProfileImageStorage(minioClient, BUCKET);
        var userId = UUID.randomUUID();

        var key = storage.store(userId, IMAGE);

        assertThat(key).startsWith("users/%s/profile-image/".formatted(userId)).endsWith(".png");
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void createsMissingBucketBeforeStoringImage() throws Exception {
        var minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));
        var storage = new MinioProfileImageStorage(minioClient, BUCKET);

        storage.store(UUID.randomUUID(), IMAGE);

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void returnsGenericApiErrorWhenStorageFails() throws Exception {
        var minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new IllegalStateException("storage full"));
        var storage = new MinioProfileImageStorage(minioClient, BUCKET);

        assertThatThrownBy(() -> storage.store(UUID.randomUUID(), IMAGE))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(exception.getMessage()).isEqualTo("Unable to store profile image");
                });
    }
}
