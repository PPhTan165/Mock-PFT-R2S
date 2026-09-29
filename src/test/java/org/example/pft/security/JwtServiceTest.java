package org.example.pft.security;

import io.jsonwebtoken.Claims;
import org.example.pft.entity.Role;
import org.example.pft.entity.User;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {
    private static final String SECRET = base64("unit-test-secret-key-32-bytes!!!");
    private static final String DIFFERENT_SECRET = base64("different-secret-key-32-bytes!!!");

    @Test
    void generateToken_withUserRoles_shouldCreateValidTokenWithExpectedClaims() {
        JwtService jwtService = new JwtService(SECRET, 3600, "pft-test");
        User user = userWithRoles("user@example.com", "USER", "ADMIN");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("user@example.com", jwtService.extractEmail(token));

        Claims claims = jwtService.parseClaim(token);
        assertEquals("pft-test", claims.getIssuer());
        assertEquals("user@example.com", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        Object roles = claims.get("roles");
        assertInstanceOf(List.class, roles);
        List<?> roleClaims = (List<?>) roles;
        assertEquals(2, roleClaims.size());
        assertTrue(roleClaims.contains("ADMIN"));
        assertTrue(roleClaims.contains("USER"));
        assertTrue(jwtService.getExpirationDateTime(token).isAfter(LocalDateTime.now()));
    }

    @Test
    void generateToken_withNullRoles_shouldCreateTokenWithEmptyRolesClaim() {
        JwtService jwtService = new JwtService(SECRET, 3600, "pft-test");
        User user = new User();
        user.setEmail("user@example.com");
        user.setRoles(null);

        String token = jwtService.generateToken(user);

        Claims claims = jwtService.parseClaim(token);
        Object roles = claims.get("roles");
        assertInstanceOf(List.class, roles);
        assertTrue(((List<?>) roles).isEmpty());
    }

    @Test
    void isTokenValid_withExpiredToken_shouldReturnFalse() {
        JwtService jwtService = new JwtService(SECRET, -60, "pft-test");
        String token = jwtService.generateToken(userWithRoles("user@example.com", "USER"));

        assertFalse(jwtService.isTokenValid(token));
        assertThrows(RuntimeException.class, () -> jwtService.parseClaim(token));
    }

    @Test
    void isTokenValid_withTokenSignedByDifferentSecret_shouldReturnFalse() {
        JwtService issuerService = new JwtService(SECRET, 3600, "pft-test");
        JwtService verifierService = new JwtService(DIFFERENT_SECRET, 3600, "pft-test");
        String token = issuerService.generateToken(userWithRoles("user@example.com", "USER"));

        assertFalse(verifierService.isTokenValid(token));
        assertThrows(RuntimeException.class, () -> verifierService.parseClaim(token));
    }

    @Test
    void isTokenValid_withMalformedBlankOrNullToken_shouldReturnFalse() {
        JwtService jwtService = new JwtService(SECRET, 3600, "pft-test");

        assertFalse(jwtService.isTokenValid("not-a-jwt"));
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    void constructor_withValidSecret_shouldCreateService() {
        assertDoesNotThrow(() -> new JwtService(SECRET, 3600, "pft-test"));
    }

    @Test
    void constructor_withBlankSecret_shouldThrowIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new JwtService(" ", 3600, "pft-test")
        );

        assertEquals("app.jwt.secret must be provided via SECRET_KEY", exception.getMessage());
    }

    @Test
    void constructor_withNullSecret_shouldThrowIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new JwtService(null, 3600, "pft-test")
        );

        assertEquals("app.jwt.secret must be provided via SECRET_KEY", exception.getMessage());
    }

    @Test
    void constructor_withNonBase64Secret_shouldThrowIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new JwtService("not-base64", 3600, "pft-test")
        );

        assertEquals("app.jwt.secret must be Base64 encoded", exception.getMessage());
    }

    @Test
    void constructor_withShortSecret_shouldThrowIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new JwtService(base64("short"), 3600, "pft-test")
        );

        assertEquals("app.jwt.secret must decode to at least 32 bytes", exception.getMessage());
    }

    private static User userWithRoles(String email, String... roleNames) {
        User user = new User();
        user.setEmail(email);
        user.setRoles(Set.of(
                role(roleNames[0]),
                role(roleNames.length > 1 ? roleNames[1] : null)
        ));
        return user;
    }

    private static Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
