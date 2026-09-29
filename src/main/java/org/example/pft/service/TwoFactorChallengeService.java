package org.example.pft.service;

import org.example.pft.entity.User;

public interface TwoFactorChallengeService {
    String createChallenge(User user);
    void resendCode(String challengeId);
    User verifyCode(String challengeId, String code);
}
