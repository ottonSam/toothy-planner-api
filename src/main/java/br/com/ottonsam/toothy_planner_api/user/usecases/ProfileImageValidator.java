package br.com.ottonsam.toothy_planner_api.user.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ProfileImageValidator {

    private static final int MAX_SIZE = 10 * 1024 * 1024;

    public ProfileImagePayload validate(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Profile image must be a valid base64 image");
        }

        byte[] content;
        try {
            content = Base64.getDecoder().decode(stripDataUrlPrefix(base64Image));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Profile image must be a valid base64 image");
        }

        if (content.length > MAX_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Profile image must be up to 10 MB");
        }

        var imageType = detectImageType(content);
        if (imageType == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Profile image format is not supported");
        }

        return new ProfileImagePayload(content, imageType.contentType(), imageType.extension());
    }

    private String stripDataUrlPrefix(String base64Image) {
        var commaIndex = base64Image.indexOf(',');
        if (base64Image.startsWith("data:") && commaIndex >= 0) {
            return base64Image.substring(commaIndex + 1);
        }
        return base64Image;
    }

    private ImageType detectImageType(byte[] content) {
        if (content.length >= 8
                && content[0] == (byte) 0x89
                && content[1] == 0x50
                && content[2] == 0x4E
                && content[3] == 0x47) {
            return new ImageType("image/png", "png");
        }
        if (content.length >= 3
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[2] == (byte) 0xFF) {
            return new ImageType("image/jpeg", "jpg");
        }
        if (content.length >= 12
                && content[0] == 0x52
                && content[1] == 0x49
                && content[2] == 0x46
                && content[3] == 0x46
                && content[8] == 0x57
                && content[9] == 0x45
                && content[10] == 0x42
                && content[11] == 0x50) {
            return new ImageType("image/webp", "webp");
        }
        if (content.length >= 12
                && content[4] == 0x66
                && content[5] == 0x74
                && content[6] == 0x79
                && content[7] == 0x70) {
            var brand = new String(content, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (brand.startsWith("heic")) {
                return new ImageType("image/heic", "heic");
            }
            if (brand.startsWith("heif") || brand.startsWith("mif1") || brand.startsWith("msf1")) {
                return new ImageType("image/heif", "heif");
            }
        }
        return null;
    }

    private record ImageType(String contentType, String extension) {}
}
