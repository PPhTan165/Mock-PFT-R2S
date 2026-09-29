package org.example.pft.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.pft.entity.Role;
import org.example.pft.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class JwtService {
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long expirationSeconds;
    private final String issuer;

    public JwtService(
           @Value("${app.jwt.secret}") String secret,
           @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds,
           @Value("${app.jwt.issuer:api}") String issuer) {
        this.signingKey = createSigningKey(secret);
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    private SecretKey createSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be provided via SECRET_KEY");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("app.jwt.secret must be Base64 encoded", ex);
        }

        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must decode to at least 32 bytes");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user){
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);

        //Roles
        List<String> roles = user.getRoles() == null ? List.of() :
                user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .toList();


        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(claims)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token){
        try{
            parseClaim(token);
            return true;
        }catch (JwtException | IllegalArgumentException ex){
            return false;
        }
    }

    public Claims parseClaim(String token){
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token){
        return parseClaim(token).getSubject();
    }

    public LocalDateTime getExpirationDateTime(String token){
        Date exp = parseClaim(token).getExpiration();
        return LocalDateTime.ofInstant(exp.toInstant(), ZoneId.systemDefault());
    }
}
