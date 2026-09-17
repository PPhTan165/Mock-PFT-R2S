package org.example.pft.service;

import org.example.pft.entity.TwoFactorChallenge;
import org.example.pft.entity.User;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.repository.TwoFactorChallengeRepository;
import org.example.pft.service.impl.TwoFactorChallengeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void resendCode_withValidChallenge_shouldResetChallengeAndSendNewOtp() {
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

        assertEquals(0, savedChallenge.getAttemptCount());
        assertTrue(savedChallenge.getExpiresAt().isAfter(beforeResend));
        assertTrue(savedChallenge.getLastSentAt().isAfter(beforeResend.minusSeconds(1)));
        assertEquals(savedChallenge.getOtpHash(), newOtp);
        assertNotNull(newOtp);
        assertTrue(newOtp.matches("\\d{6}"));
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
}
