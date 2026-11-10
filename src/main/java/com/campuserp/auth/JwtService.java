package com.campuserp.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration lifetime;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

    /**
     * The token carries a signed fingerprint of the password hash, so changing
     * the password makes every token issued before it stop working.
     */
    public String createToken(String username, String passwordHash) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("pw", sign(passwordHash))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key)
                .compact();
    }

    /**
     * A short signature for text, made with the same secret as login tokens.
     * Used for links that can't carry a login, like calendar feeds.
     */
    public String sign(String text) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(key.getAlgorithm());
            mac.init(key);
            byte[] sig = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(java.util.Arrays.copyOf(sig, 24));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean checkSignature(String text, String signature) {
        return signature != null && java.security.MessageDigest.isEqual(
                sign(text).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    /** The token's claims, or empty if the token is bad or expired. */
    public Optional<Claims> read(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** True if the token was issued for this password (false after a password change). */
    public boolean matchesPassword(Claims claims, String passwordHash) {
        return checkSignature(passwordHash, claims.get("pw", String.class));
    }
}
