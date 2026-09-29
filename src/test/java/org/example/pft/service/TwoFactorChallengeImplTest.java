package org.example.pft.service;

import org.example.pft.entity.TwoFactorChallenge;
import org.example.pft.entity.User;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.repository.TwoFactorChallengeRepository;
import org.example.pft.service.impl.TwoFactorChallengeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFactorChallengeImplTest {

    @Mock
    TwoFactorChallengeRepository twoFactorChallengeRepository;

    @Mock
    EmailService emailService;

    @InjectMocks
    TwoFactorChallengeImpl twoFactorChallengeService;

    private User user;
    private TwoFactorChallenge challenge;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        challenge = new TwoFactorChallenge();
        challenge.setId(1L);
        challenge.setUser(user);
        challenge.setChallengeId("challenge-123");
        challenge.setOtpHash("123456");
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setAttemptCount(0);
        challenge.setLastSentAt(LocalDateTime.now().minusMinutes(2));
        challenge.setUsed(false);
        challenge.setCreatedAt(LocalDateTime.now().minusMinutes(2));
    }

    @Test
    void createChallenge_withoutExistingChallenge_shouldCreateChallengeAndSendOtp() {
        when(twoFactorChallengeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());

        String challengeId = twoFactorChallengeService.createChallenge(user);

        ArgumentCaptor<TwoFactorChallenge> challengeCaptor =
                ArgumentCaptor.forClass(TwoFactorChallenge.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);

        verify(twoFactorChallengeRepository)
                .save(challengeCaptor.capture());
        verify(emailService)
                .sendOtpMail(eq("user@example.com"), otpCaptor.capture());

        TwoFactorChallenge savedChallenge = challengeCaptor.getValue();
        String otpCode = otpCaptor.getValue();

        assertEquals(challengeId, savedChallenge.getChallengeId());
        assertEquals(user, savedChallenge.getUser());
        assertEquals(0, savedChallenge.getAttemptCount());
        assertFalse(savedChallenge.getUsed());
        assertEquals(savedChallenge.getOtpHash(), otpCode);
        assertTrue(otpCode.matches("\\d{6}"));
    }

    @Test
    void createChallenge_withValidChallengeDuringCooldown_shouldReturnExistingChallengeId() {
        challenge.setLastSentAt(LocalDateTime.now());

        when(twoFactorChallengeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(challenge));

        String challengeId = twoFactorChallengeService.createChallenge(user);

        assertEquals("challenge-123", challengeId);

        verify(twoFactorChallengeRepository, never())
                .save(any());
        verify(emailService, never())
                .sendOtpMail(any(), any());
    }

    @Test
    void createChallenge_withExpiredChallenge_shouldCloseOldChallengeAndCreateNewChallenge() {
        challenge.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(twoFactorChallengeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(challenge));

        String challengeId = twoFactorChallengeService.createChallenge(user);

        ArgumentCaptor<TwoFactorChallenge> challengeCaptor =
                ArgumentCaptor.forClass(TwoFactorChallenge.class);

        verify(twoFactorChallengeRepository, times(2))
                .save(challengeCaptor.capture());
        verify(emailService)
                .sendOtpMail(eq("user@example.com"), any());

        TwoFactorChallenge oldChallenge = challengeCaptor.getAllValues().get(0);
        TwoFactorChallenge newChallenge = challengeCaptor.getAllValues().get(1);

        assertTrue(oldChallenge.getUsed());
        assertEquals(challengeId, newChallenge.getChallengeId());
        assertFalse(newChallenge.getUsed());
    }

    @Test
    void createChallenge_afterCooldown_shouldReplaceOldChallenge() {
        challenge.setExpiresAt(LocalDateTime.of(2099, 1, 1, 0, 0));
        challenge.setLastSentAt(LocalDateTime.of(2000, 1, 1, 0, 0));

        when(twoFactorChallengeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(challenge));

        String challengeId = twoFactorChallengeService.createChallenge(user);

        ArgumentCaptor<TwoFactorChallenge> challengeCaptor =
                ArgumentCaptor.forClass(TwoFactorChallenge.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);

        verify(twoFactorChallengeRepository, times(2))
                .save(challengeCaptor.capture());
        verify(emailService)
                .sendOtpMail(eq("user@example.com"), otpCaptor.capture());

        TwoFactorChallenge oldChallenge = challengeCaptor.getAllValues().get(0);
        TwoFactorChallenge newChallenge = challengeCaptor.getAllValues().get(1);
        String otpCode = otpCaptor.getValue();

        assertSame(challenge, oldChallenge);
        assertTrue(oldChallenge.getUsed());

        assertEquals(challengeId, newChallenge.getChallengeId());
        assertNotEquals("challenge-123", newChallenge.getChallengeId());
        assertEquals(user, newChallenge.getUser());
        assertEquals(0, newChallenge.getAttemptCount());
        assertFalse(newChallenge.getUsed());
        assertEquals(newChallenge.getOtpHash(), otpCode);
        assertTrue(otpCode.matches("\\d{6}"));
    }

    @Test
    void createChallenge_withExhaustedChallengeDuringCooldown_shouldRejectAndNotSendOtp() {
        challenge.setAttemptCount(5);
        challenge.setLastSentAt(LocalDateTime.now());
        LocalDateTime originalExpiresAt = challenge.getExpiresAt();

        when(twoFactorChallengeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(challenge));

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.createChallenge(user)
        );

        assertTrue(exception.getMessage().startsWith("Please wait "));
        assertEquals(5, challenge.getAttemptCount());
        assertEquals(originalExpiresAt, challenge.getExpiresAt());
        assertFalse(challenge.getUsed());

        verify(twoFactorChallengeRepository, never())
                .save(any());
        verify(emailService, never())
                .sendOtpMail(any(), any());
    }

    @Test
    void createChallenge_withExhaustedChallengeAfterCooldown_shouldInvalidateOldChallengeAndCreateNewChallenge() {
        challenge.setAttemptCount(5);
        challenge.setExpiresAt(LocalDateTime.of(2099, 1, 1, 0, 0));
        challenge.setLastSentAt(LocalDateTime.of(2000, 1, 1, 0, 0));

        when(twoFactorChallengeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(challenge));

        String challengeId = twoFactorChallengeService.createChallenge(user);

        ArgumentCaptor<TwoFactorChallenge> challengeCaptor =
                ArgumentCaptor.forClass(TwoFactorChallenge.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);

        verify(twoFactorChallengeRepository, times(2))
                .save(challengeCaptor.capture());
        verify(emailService)
                .sendOtpMail(eq("user@example.com"), otpCaptor.capture());

        TwoFactorChallenge oldChallenge = challengeCaptor.getAllValues().get(0);
        TwoFactorChallenge newChallenge = challengeCaptor.getAllValues().get(1);
        String otpCode = otpCaptor.getValue();

        assertSame(challenge, oldChallenge);
        assertTrue(oldChallenge.getUsed());
        assertEquals(5, oldChallenge.getAttemptCount());

        assertEquals(challengeId, newChallenge.getChallengeId());
        assertNotEquals("challenge-123", newChallenge.getChallengeId());
        assertEquals(0, newChallenge.getAttemptCount());
        assertFalse(newChallenge.getUsed());
        assertEquals(newChallenge.getOtpHash(), otpCode);
        assertTrue(otpCode.matches("\\d{6}"));
    }

    @Test
    void verifyCode_withOldChallengeAfterReplacement_shouldRejectOldAndVerifyFreshChallenge() {
        challenge.setAttemptCount(5);
        challenge.setExpiresAt(LocalDateTime.of(2099, 1, 1, 0, 0));
        challenge.setLastSentAt(LocalDateTime.of(2000, 1, 1, 0, 0));

        when(twoFactorChallengeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(challenge));

        String newChallengeId = twoFactorChallengeService.createChallenge(user);

        ArgumentCaptor<TwoFactorChallenge> challengeCaptor =
                ArgumentCaptor.forClass(TwoFactorChallenge.class);

        verify(twoFactorChallengeRepository, times(2))
                .save(challengeCaptor.capture());

        TwoFactorChallenge oldChallenge = challengeCaptor.getAllValues().get(0);
        TwoFactorChallenge newChallenge = challengeCaptor.getAllValues().get(1);

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(oldChallenge));
        when(twoFactorChallengeRepository.findByChallengeId(newChallengeId))
                .thenReturn(Optional.of(newChallenge));

        BusinessValidationException oldChallengeException = assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.verifyCode("challenge-123", "123456")
        );

        assertEquals("Two-factor challenge has already been used", oldChallengeException.getMessage());
        assertTrue(oldChallenge.getUsed());

        User result = twoFactorChallengeService.verifyCode(newChallengeId, newChallenge.getOtpHash());

        assertSame(user, result);
        assertTrue(newChallenge.getUsed());
    }

    @Test
    void verifyCode_withValidCode_shouldMarkChallengeUsedAndReturnUser() {
        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        User result = twoFactorChallengeService.verifyCode("challenge-123", "123456");

        assertSame(user, result);
        assertTrue(challenge.getUsed());

        verify(twoFactorChallengeRepository)
                .save(challenge);
    }

    @Test
    void verifyCode_whenChallengeNotFound_shouldThrowNotFound() {
        when(twoFactorChallengeRepository.findByChallengeId("missing-challenge"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> twoFactorChallengeService.verifyCode("missing-challenge", "123456")
        );

        verify(twoFactorChallengeRepository, never())
                .save(any());
    }

    @Test
    void verifyCode_whenChallengeAlreadyUsed_shouldReject() {
        challenge.setUsed(true);
        challenge.setAttemptCount(2);

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.verifyCode("challenge-123", "123456")
        );

        assertEquals("Two-factor challenge has already been used", exception.getMessage());
        assertEquals(2, challenge.getAttemptCount());
        assertTrue(challenge.getUsed());

        verify(twoFactorChallengeRepository, never())
                .save(any());
    }

    @Test
    void verifyCode_whenChallengeExpired_shouldReject() {
        challenge.setExpiresAt(LocalDateTime.of(2000, 1, 1, 0, 0));

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.verifyCode("challenge-123", "123456")
        );

        assertEquals("Verification code has expired", exception.getMessage());
        assertEquals(0, challenge.getAttemptCount());
        assertFalse(challenge.getUsed());

        verify(twoFactorChallengeRepository, never())
                .save(any());
    }

    @Test
    void verifyCode_whenMaxAttemptsReached_shouldReject() {
        challenge.setAttemptCount(5);

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.verifyCode("challenge-123", "123456")
        );

        assertEquals("Too many verification attempts", exception.getMessage());
        assertEquals(5, challenge.getAttemptCount());
        assertFalse(challenge.getUsed());

        verify(twoFactorChallengeRepository, never())
                .save(any());
    }

    @Test
    void verifyCode_onFifthIncorrectAttempt_shouldReachLimit() {
        challenge.setAttemptCount(4);

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.verifyCode("challenge-123", "000000")
        );

        assertEquals("Too many verification attempts", exception.getMessage());
        assertEquals(5, challenge.getAttemptCount());
        assertFalse(challenge.getUsed());

        verify(twoFactorChallengeRepository)
                .save(challenge);
    }

    @Test
    void verifyCode_withInvalidCode_shouldIncreaseAttemptAndThrowException() {
        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.verifyCode("challenge-123", "000000")
        );

        assertEquals(1, challenge.getAttemptCount());
        assertFalse(challenge.getUsed());

        verify(twoFactorChallengeRepository)
                .save(challenge);
    }

    @Test
    void resendCode_withValidChallenge_shouldPreserveAttemptsAndSendNewOtp() {
        challenge.setAttemptCount(3);
        LocalDateTime beforeResend = LocalDateTime.now();

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        twoFactorChallengeService.resendCode("challenge-123");

        ArgumentCaptor<TwoFactorChallenge> challengeCaptor =
                ArgumentCaptor.forClass(TwoFactorChallenge.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);

        verify(twoFactorChallengeRepository)
                .save(challengeCaptor.capture());
        verify(emailService)
                .sendOtpMail(eq("user@example.com"), otpCaptor.capture());

        TwoFactorChallenge savedChallenge = challengeCaptor.getValue();
        String newOtp = otpCaptor.getValue();

        assertEquals(3, savedChallenge.getAttemptCount());
        assertTrue(savedChallenge.getExpiresAt().isAfter(beforeResend));
        assertTrue(savedChallenge.getLastSentAt().isAfter(beforeResend.minusSeconds(1)));
        assertEquals(savedChallenge.getOtpHash(), newOtp);
        assertNotNull(newOtp);
        assertTrue(newOtp.matches("\\d{6}"));
    }

    @Test
    void resendCode_whenMaxAttemptsReached_shouldRejectAndNotSendEmail() {
        challenge.setAttemptCount(5);
        String originalOtp = challenge.getOtpHash();
        LocalDateTime originalExpiresAt = challenge.getExpiresAt();
        LocalDateTime originalLastSentAt = challenge.getLastSentAt();

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.resendCode("challenge-123")
        );

        assertEquals("Too many verification attempts", exception.getMessage());
        assertEquals(5, challenge.getAttemptCount());
        assertEquals(originalOtp, challenge.getOtpHash());
        assertEquals(originalExpiresAt, challenge.getExpiresAt());
        assertEquals(originalLastSentAt, challenge.getLastSentAt());

        verify(twoFactorChallengeRepository, never())
                .save(any());
        verify(emailService, never())
                .sendOtpMail(any(), any());
    }

    @Test
    void verifyCode_withOldOtpAfterResend_shouldRejectOldOtpAndVerifyNewOtpWithoutResettingAttempts() {
        useOtpValues(222222);
        challenge.setOtpHash("111111");
        challenge.setAttemptCount(2);

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        twoFactorChallengeService.resendCode("challenge-123");

        assertEquals("222222", challenge.getOtpHash());
        assertEquals(2, challenge.getAttemptCount());

        BusinessValidationException oldOtpException = assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.verifyCode("challenge-123", "111111")
        );

        assertEquals("Invalid verification code", oldOtpException.getMessage());
        assertEquals(3, challenge.getAttemptCount());
        assertFalse(challenge.getUsed());

        User result = twoFactorChallengeService.verifyCode("challenge-123", "222222");

        assertSame(user, result);
        assertEquals(3, challenge.getAttemptCount());
        assertTrue(challenge.getUsed());

        verify(emailService)
                .sendOtpMail(eq("user@example.com"), eq("222222"));
    }

    @Test
    void resendCode_duringCooldown_shouldThrowExceptionAndNotSendEmail() {
        challenge.setLastSentAt(LocalDateTime.now());

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.resendCode("challenge-123")
        );

        verify(twoFactorChallengeRepository, never())
                .save(any());
        verify(emailService, never())
                .sendOtpMail(any(), any());
    }

    @Test
    void resendCode_withUsedChallenge_shouldThrowExceptionAndNotSendEmail() {
        challenge.setUsed(true);

        when(twoFactorChallengeRepository.findByChallengeId("challenge-123"))
                .thenReturn(Optional.of(challenge));

        assertThrows(
                BusinessValidationException.class,
                () -> twoFactorChallengeService.resendCode("challenge-123")
        );

        verify(twoFactorChallengeRepository, never())
                .save(any());
        verify(emailService, never())
                .sendOtpMail(any(), any());
    }

    private void useOtpValues(int... otpValues) {
        try {
            Field secureRandomField = TwoFactorChallengeImpl.class.getDeclaredField("secureRandom");
            secureRandomField.setAccessible(true);
            secureRandomField.set(twoFactorChallengeService, new FixedSecureRandom(otpValues));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static class FixedSecureRandom extends SecureRandom {
        private final int[] otpValues;
        private int index;

        FixedSecureRandom(int... otpValues) {
            this.otpValues = otpValues;
        }

        @Override
        public int nextInt(int bound) {
            int otpValue = otpValues[Math.min(index, otpValues.length - 1)];
            index++;

            return otpValue;
        }
    }
}
