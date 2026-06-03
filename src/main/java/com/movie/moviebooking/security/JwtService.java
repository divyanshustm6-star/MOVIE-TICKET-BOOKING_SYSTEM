package com.movie.moviebooking.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.moviebooking.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long expirationSeconds;
    private final String issuer;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret:change-this-secret-to-a-long-random-production-value}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds,
            @Value("${app.jwt.issuer:moviebooking}") String issuer) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    public String generateToken(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", JWT_ALGORITHM);
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", issuer);
        payload.put("sub", user.getEmail());
        payload.put("uid", user.getId());
        payload.put("roles", roles);
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);

        return encode(header, payload);
    }

    public String extractSubject(String token) {
        Object subject = readPayload(token).get("sub");
        if (!(subject instanceof String email) || email.isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing");
        }
        return email;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public boolean isValid(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                return false;
            }
            Map<String, Object> header = readJson(parts[0]);
            if (!JWT_ALGORITHM.equals(header.get("alg"))) {
                return false;
            }

            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.US_ASCII),
                    parts[2].getBytes(StandardCharsets.US_ASCII))) {
                return false;
            }

            Map<String, Object> payload = readJson(parts[1]);
            if (!issuer.equals(payload.get("iss"))) {
                return false;
            }
            Object expiresAt = payload.get("exp");
            Object subject = payload.get("sub");
            return expiresAt instanceof Number exp
                    && subject instanceof String email
                    && !email.isBlank()
                    && Instant.now().getEpochSecond() < exp.longValue();
        } catch (Exception ex) {
            return false;
        }
    }

    private String encode(Map<String, Object> header, Map<String, Object> payload) {
        try {
            String unsigned = base64Url(objectMapper.writeValueAsBytes(header))
                    + "." + base64Url(objectMapper.writeValueAsBytes(payload));
            return unsigned + "." + sign(unsigned);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate JWT", ex);
        }
    }

    private Map<String, Object> readPayload(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT");
        }
        return readJson(parts[1]);
    }

    private Map<String, Object> readJson(String base64UrlJson) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(base64UrlJson);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JWT payload", ex);
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
