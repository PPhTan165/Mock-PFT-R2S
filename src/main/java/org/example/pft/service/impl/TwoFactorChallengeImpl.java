package org.example.pft.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.pft.entity.TwoFactorChallenge;
import org.example.pft.entity.User;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.repository.TwoFactorChallengeRepository;
import org.example.pft.service.EmailService;
import org.example.pft.service.TwoFactorChallengeService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TwoFactorChallengeImpl implements TwoFactorChallengeService {

    private static final int OTP_EXPIRE_MINUTES = 5;
    private static final int MAX_ATTEMPTS =5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final TwoFactorChallengeRepository twoFactorChallengeRepository;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String createChallenge(User user) {
        LocalDateTime now = LocalDateTime.now();

        Optional<TwoFactorChallenge> latestChallenge =
                twoFactorChallengeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId());

        if (latestChallenge.isPresent()) {
            TwoFactorChallenge challenge = latestChallenge.get();

            boolean invalidChallenge = checkInvalidChallenge(challenge);
            boolean stillInCooldown = challenge.getLastSentAt()
                    .plusSeconds(RESEND_COOLDOWN_SECONDS)
                    .isAfter(now);

            if (!invalidChallenge && stillInCooldown) {
                return challenge.getChallengeId();
            }

            challenge.setUsed(true);
            twoFactorChallengeRepository.save(challenge);
        }

        return createNewChallenge(user, now);
    }

    private String createNewChallenge(User user, LocalDateTime now) {
        String challengeId = UUID.randomUUID().toString();
        String otpCode = generateOTP();

        TwoFactorChallenge newChallenge = new TwoFactorChallenge();

        newChallenge.setUser(user);
        newChallenge.setChallengeId(challengeId);
        newChallenge.setOtpHash(otpCode);

        newChallenge.setExpiresAt(now.plusMinutes(OTP_EXPIRE_MINUTES));
        newChallenge.setAttemptCount(0);
        newChallenge.setLastSentAt(now);
        newChallenge.setUsed(false);
        newChallenge.setCreatedAt(now);

        twoFactorChallengeRepository.save(newChallenge);

        emailService.sendOtpMail(user.getEmail(),otpCode);

        return challengeId;
    }

    @Override
    public void resendCode(String challengeId) {
        TwoFactorChallenge challenge = twoFactorChallengeRepository.findByChallengeId(challengeId)
                .orElseThrow(()-> new ResourceNotFoundException("Two-factor challenge not found"));

        LocalDateTime now = LocalDateTime.now();

        if(Boolean.TRUE.equals(challenge.getUsed())){
            throw new BusinessValidationException("Two-factor challenge has already been used");
        }

        LocalDateTime availableAt =
                challenge.getLastSentAt()
                        .plusSeconds(RESEND_COOLDOWN_SECONDS);

        if (availableAt.isAfter(now)) {

            long remainingSeconds =
                    Duration.between(
                            now,
                            availableAt
                    ).getSeconds();

            throw new BusinessValidationException(
                    "Please wait "
                            + remainingSeconds
                            + " seconds before requesting another code"
            );
        }

        String newOtp = generateOTP();

        challenge.setOtpHash(newOtp);
        challenge.setAttemptCount(0);
        challenge.setExpiresAt(now.plusMinutes(OTP_EXPIRE_MINUTES));
        challenge.setLastSentAt(now);

        twoFactorChallengeRepository.save(challenge);

        emailService.sendOtpMail(challenge.getUser().getEmail(),newOtp);

    }

    @Override
    public User verifyCode(String challengeId, String code) {
        TwoFactorChallenge challenge = twoFactorChallengeRepository.findByChallengeId(challengeId)
                .orElseThrow(()-> new ResourceNotFoundException("Two-factor challenge not found"));

        LocalDateTime now = LocalDateTime.now();

        if(Boolean.TRUE.equals(challenge.getUsed())){
            throw new BusinessValidationException("Two-factor challenge has already been used");
        }

        if (challenge.getExpiresAt().isBefore(now)) {
            throw new BusinessValidationException("Verification code has expired");
        }

        if (challenge.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BusinessValidationException("Too many verification attempts");
        }

        if(!challenge.getOtpHash().equals(code)){
            int newAttemptCount = challenge.getAttemptCount() + 1;
            challenge.setAttemptCount(newAttemptCount);

            twoFactorChallengeRepository.save(challenge);

            if(newAttemptCount >= MAX_ATTEMPTS){
                throw new BusinessValidationException("Too many verification attempts");
            }

            throw new BusinessValidationException("Invalid verification code");
        }

        challenge.setUsed(true);
        twoFactorChallengeRepository.save(challenge);

        return challenge.getUser();
    }

    private String generateOTP(){
        int otp = secureRandom.nextInt(1_000_000);

        return String.format(
                "%06d",
                otp
        );

    }

    private boolean checkInvalidChallenge(TwoFactorChallenge challenge){
        return Boolean.TRUE.equals(challenge.getUsed())
                || !challenge.getExpiresAt().isAfter(LocalDateTime.now());
    }
}
