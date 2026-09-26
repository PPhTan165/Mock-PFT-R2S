package org.example.pft.helper;

import org.example.pft.entity.User;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserHelperTest {
    private static final String EMAIL = "user@example.com";

    @Mock
    UserRepository userRepository;

    CurrentUserHelper currentUserHelper;

    @BeforeEach
    void setup() {
        currentUserHelper = new CurrentUserHelper(userRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthentication_withoutAuthentication_shouldThrow() {
        AuthenticationCredentialsNotFoundException exception = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> currentUserHelper.getAuthentication()
        );

        assertEquals("Authentication required", exception.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    void getAuthentication_withAnonymousUser_shouldReject() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "anonymous-key",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                )
        );

        AuthenticationCredentialsNotFoundException exception = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> currentUserHelper.getAuthentication()
        );

        assertEquals("Authentication required", exception.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    void getAuthentication_withUnauthenticatedUser_shouldReject() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null)
        );

        AuthenticationCredentialsNotFoundException exception = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> currentUserHelper.getAuthentication()
        );

        assertEquals("Authentication required", exception.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    void getCurrentEmailUser_withAuthenticatedUserDetails_shouldReturnEmail() {
        setAuthenticatedUserDetails(EMAIL);

        String email = currentUserHelper.getCurrentEmailUser();

        assertEquals(EMAIL, email);
        verifyNoInteractions(userRepository);
    }

    @Test
    void getCurrentEmailUser_withAnonymousUserName_shouldThrow() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "anonymousUser",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        AuthenticationCredentialsNotFoundException exception = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> currentUserHelper.getCurrentEmailUser()
        );

        assertEquals("Authentication required", exception.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    void getCurrentUser_withUnknownEmail_shouldThrow() {
        setAuthenticatedUserDetails(EMAIL);
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> currentUserHelper.getCurrentUser()
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void getCurrentUser_withValidAuthentication_shouldReturnUser() {
        setAuthenticatedUserDetails(EMAIL);
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        User result = currentUserHelper.getCurrentUser();

        assertSame(user, result);
        verify(userRepository).findByEmail(EMAIL);
    }

    private static void setAuthenticatedUserDetails(String email) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );
    }
}
