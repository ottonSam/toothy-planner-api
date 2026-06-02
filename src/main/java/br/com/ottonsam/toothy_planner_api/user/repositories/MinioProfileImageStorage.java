package br.com.ottonsam.toothy_planner_api.user.repositories;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImageData;
import br.com.ottonsam.toothy_planner_api.user.usecases.ProfileImagePayload;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MinioProfileImageStorage implements ProfileImageStorage {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioProfileImageStorage(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @Override
    public String store(UUID userId, ProfileImagePayload image) {
        try {
            ensureBucketExists();
            var key = "users/%s/profile-image/%s.%s".formatted(userId, UUID.randomUUID(), image.extension());
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(
                            new ByteArrayInputStream(image.content()), (long) image.content().length, -1L)
                    .contentType(image.contentType())
                    .build());
            return key;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store profile image");
        }
    }

    @Override
    public Optional<ProfileImageData> load(String key) {
        try {
            var stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(key).build());
            try (var stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(key).build())) {
                return Optional.of(new ProfileImageData(stream.readAllBytes(), stat.contentType()));
            }
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete profile image");
        }
    }

    private void ensureBucketExists() throws Exception {
        var exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
