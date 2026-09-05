package org.example.pft.service;

import org.example.pft.dto.login.LoginRequest;
import org.example.pft.dto.login.LoginResponse;
import org.example.pft.entity.User;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.repository.UserRepository;
import org.example.pft.security.JwtService;
import org.example.pft.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthServiceImpl authService;

    private User user;
    private LoginRequest request;

    @BeforeEach
    void setup(){
        request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("123456");

        user = new User();
        user.setId(1);
        user.setEmail("user@example.com");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
    }

    @Test
    void login_withValidCredentials_shouldReturnToken(){
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "123456",
                "encoded-password"
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("mock-jwt-token");
        when(jwtService.getExpirationDateTime("mock-jwt-token"))
                .thenReturn(LocalDateTime.of(2026, 9, 5, 15, 0));

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token",response.getData().getAccessToken());

        verify(userRepository).findByEmail("user@example.com");
        verify(passwordEncoder).matches(
                "123456",
                "encoded-password"
        );
        verify(jwtService).generateToken(user);
    }

    @Test
    void login_withEmailNotFound_shouldThrowException(){
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@gmail.com");
        request.setPassword("123456");

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessValidationException.class,
                ()-> authService.login(request)
        );

        verify(userRepository).findByEmail("unknown@gmail.com");

        verify(passwordEncoder,never())
                .matches(anyString(), anyString());

        verify(jwtService,never())
                .generateToken(any());
    }

    @Test
    void login_withWrongPassword_shouldThrowException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@gmail.com");
        request.setPassword("wrong-password");

        User user = new User();
        user.setEmail("user@gmail.com");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(0);

        when(userRepository.findByEmail("user@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        assertThrows(
                BusinessValidationException.class,
                () -> authService.login(request)
        );

        verify(jwtService, never())
                .generateToken(any());
    }

    @Test
    void login_whenAccountIsLocked_shouldThrowException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@gmail.com");
        request.setPassword("123456");

        User user = new User();
        user.setEmail("user@gmail.com");
        user.setPassword("encoded-password");
        user.setLockedUntil(
                LocalDateTime.now().plusMinutes(20)
        );

        when(userRepository.findByEmail("user@gmail.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                BusinessValidationException.class,
                () -> authService.login(request)
        );

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(any());
    }

    @Test
    void login_fifthWrongAttempt_shouldLockAccount() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("user@gmail.com");
        request.setPassword("wrong-password");

        User user = new User();
        user.setEmail("user@gmail.com");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(4);
        user.setLockedUntil(null);

        when(userRepository.findByEmail("user@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        // Act + Assert
        assertThrows(
                BusinessValidationException.class,
                () -> authService.login(request)
        );

        // Kiểm tra failed attempts
        assertEquals(5, user.getFailedLoginAttempts());

        // Kiểm tra account đã bị lock
        assertNotNull(user.getLockedUntil());

        // Kiểm tra user được update xuống database
        verify(userRepository).save(user);

        // Password sai thì tuyệt đối không tạo JWT
        verify(jwtService, never()).generateToken(any());
    }
}
