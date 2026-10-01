package org.example.pft.service;

import org.example.pft.dto.auth.LoginRequest;
import org.example.pft.dto.auth.LoginResponse;
import org.example.pft.dto.twoFactor.ResendTwoFactorRequest;
import org.example.pft.dto.twoFactor.VerifyTwoFactorRequest;
import org.example.pft.entity.User;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.EmailNotFoundException;
import org.example.pft.repository.RoleRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {
    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    TwoFactorChallengeService twoFactorChallengeService;

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
        user.setId(1L);
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
    void login_withTwoFactorEnabled_shouldReturnChallengeWithoutToken() {
        user.setTwoFactorEnabled(true);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "123456",
                "encoded-password"
        )).thenReturn(true);

        when(twoFactorChallengeService.createChallenge(user))
                .thenReturn("challenge-123");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Two-factor verification required", response.getMessage());
        assertEquals("challenge-123", response.getChallengeId());
        assertNull(response.getData());

        verify(twoFactorChallengeService)
                .createChallenge(user);
        verify(jwtService, never())
                .generateToken(any());
    }

    @Test
    void login_withEmailNotFound_shouldThrowException(){
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@gmail.com");
        request.setPassword("123456");

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        EmailNotFoundException exception = assertThrows(
                EmailNotFoundException.class,
                () -> authService.login(request)
        );

        assertEquals("Email does not exists", exception.getMessage());

        verify(userRepository).findByEmail("unknown@gmail.com");

        verify(passwordEncoder,never())
                .matches(anyString(), anyString());

        verify(jwtService,never())
                .generateToken(any());
    }

    @Test
    void login_withWrongPasswordBeforeLimit_shouldIncrementAttempts() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@gmail.com");
        request.setPassword("wrong-password");

        User user = new User();
        user.setEmail("user@gmail.com");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(2);
        user.setLockedUntil(null);

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

        assertEquals(3, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());

        verify(userRepository).save(user);
        verify(jwtService, never())
                .generateToken(any());
        verify(twoFactorChallengeService, never())
                .createChallenge(any());
    }

    @Test
    void login_whenAccountIsLocked_shouldThrowException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@gmail.com");
        request.setPassword("123456");

        User user = new User();
        user.setEmail("user@gmail.com");
        user.setPassword("encoded-password");
        user.setFailedLoginAttempts(5);

        LocalDateTime lockedUntil = LocalDateTime.of(2099, 1, 1, 0, 0);
        user.setLockedUntil(lockedUntil);

        when(userRepository.findByEmail("user@gmail.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                BusinessValidationException.class,
                () -> authService.login(request)
        );

        assertEquals(5, user.getFailedLoginAttempts());
        assertEquals(lockedUntil, user.getLockedUntil());

        verify(userRepository, never())
                .save(any());
        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(any());
        verify(twoFactorChallengeService, never())
                .createChallenge(any());
    }

    @Test
    void login_whenLockExpiredWithValidPassword_shouldResetFailureStateAndReturnToken() {
        LocalDateTime expiredLock = LocalDateTime.of(2000, 1, 1, 0, 0);
        user.setFailedLoginAttempts(5);
        user.setLockedUntil(expiredLock);

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
        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertNull(response.getChallengeId());
        assertEquals("mock-jwt-token", response.getData().getAccessToken());

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());

        verify(userRepository).save(user);
        verify(jwtService).generateToken(user);
        verify(twoFactorChallengeService, never())
                .createChallenge(any());
    }

    @Test
    void login_whenLockExpiredWithWrongPassword_shouldResetThenIncrementFailedAttempts() {
        LocalDateTime expiredLock = LocalDateTime.of(2000, 1, 1, 0, 0);
        user.setFailedLoginAttempts(5);
        user.setLockedUntil(expiredLock);
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        assertThrows(
                BusinessValidationException.class,
                () -> authService.login(request)
        );

        assertEquals(1, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());

        verify(userRepository, times(2))
                .save(user);
        verify(jwtService, never())
                .generateToken(any());
        verify(twoFactorChallengeService, never())
                .createChallenge(any());
    }

    @Test
    void login_afterPreviousFailures_shouldResetFailureState() {
        user.setFailedLoginAttempts(3);
        user.setLockedUntil(null);

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
        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals("mock-jwt-token", response.getData().getAccessToken());

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());

        verify(userRepository).save(user);
        verify(jwtService).generateToken(user);
        verify(twoFactorChallengeService, never())
                .createChallenge(any());
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

    @Test
    void verifyTwoFactor_withValidCode_shouldReturnToken() {
        VerifyTwoFactorRequest request = new VerifyTwoFactorRequest();
        request.setChallengeId("challenge-123");
        request.setCode("123456");

        when(twoFactorChallengeService.verifyCode("challenge-123", "123456"))
                .thenReturn(user);
        when(jwtService.generateToken(user))
                .thenReturn("mock-jwt-token");
        when(jwtService.getExpirationDateTime("mock-jwt-token"))
                .thenReturn(LocalDateTime.of(2026, 9, 5, 15, 0));

        LoginResponse response = authService.verifyTwoFactor(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertNull(response.getChallengeId());
        assertEquals("mock-jwt-token", response.getData().getAccessToken());

        verify(twoFactorChallengeService)
                .verifyCode("challenge-123", "123456");
        verify(jwtService).generateToken(user);
    }

    @Test
    void resendTwoFactorCode_withValidChallenge_shouldDelegateToTwoFactorService() {
        ResendTwoFactorRequest request = new ResendTwoFactorRequest();
        request.setChallengeId("challenge-123");

        authService.resendTwoFactorCode(request);

        verify(twoFactorChallengeService)
                .resendCode("challenge-123");
    }
}
