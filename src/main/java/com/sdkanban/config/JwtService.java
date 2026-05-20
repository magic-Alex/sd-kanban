package com.sdkanban.config;

import com.sdkanban.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expiresMinutes;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expires-minutes}") long expiresMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiresMinutes = expiresMinutes;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(user.getAccount())
            .claim("userId", user.getId())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiresMinutes, ChronoUnit.MINUTES)))
            .signWith(signingKey)
            .compact();
    }

    public String parseAccount(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

}
