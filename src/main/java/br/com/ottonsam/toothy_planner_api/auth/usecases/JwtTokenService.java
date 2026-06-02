package br.com.ottonsam.toothy_planner_api.auth.usecases;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    public static final String ACCESS_TYPE = "access";
    public static final String REFRESH_TYPE = "refresh";

    private static final Base64.Encoder BASE64_URL_ENCODER =
            Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final String secret;
    private final String issuer;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer) {
        this.objectMapper = objectMapper.copy();
        this.secret = secret;
        this.issuer = issuer;
    }

    public AuthTokens createTokens(UUID userId) {
        return new AuthTokens(
                createToken(userId, ACCESS_TYPE, Instant.now().plusSeconds(15 * 60)),
                createToken(userId, REFRESH_TYPE, Instant.now().plusSeconds(2 * 24 * 60 * 60)));
    }

    public UUID validate(String token, String expectedType) {
        try {
            var parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            var unsignedToken = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
                return null;
            }
            Map<String, Object> claims = objectMapper.readValue(BASE64_URL_DECODER.decode(parts[1]), CLAIMS_TYPE);
            if (!issuer.equals(claims.get("iss")) || !expectedType.equals(claims.get("typ"))) {
                return null;
            }
            var expiresAt = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= expiresAt) {
                return null;
            }
            return UUID.fromString((String) claims.get("sub"));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
            return null;
        }
    }

    private String createToken(UUID userId, String type, Instant expiresAt) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload =
                    Map.of("iss", issuer, "sub", userId.toString(), "typ", type, "exp", expiresAt.getEpochSecond());
            var unsignedToken = encodeJson(header) + "." + encodeJson(payload);
            return unsignedToken + "." + sign(unsignedToken);
        } catch (GeneralSecurityException | JsonProcessingException exception) {
            throw new IllegalStateException("Unable to create token", exception);
        }
    }

    private String encodeJson(Map<String, Object> value) throws JsonProcessingException {
        return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws GeneralSecurityException {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected.length() != actual.length()) {
            return false;
        }
        var result = 0;
        for (var index = 0; index < expected.length(); index++) {
            result |= expected.charAt(index) ^ actual.charAt(index);
        }
        return result == 0;
    }
}
