package org.example.pft.service;

import org.example.pft.dto.auth.LoginRequest;
import org.example.pft.dto.auth.LoginResponse;
import org.example.pft.dto.auth.RegisterRequest;
import org.example.pft.dto.auth.RegisterResponse;
import org.example.pft.dto.twoFactor.ResendTwoFactorRequest;
import org.example.pft.dto.twoFactor.VerifyTwoFactorRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    RegisterResponse register(RegisterRequest request);
    LoginResponse verifyTwoFactor(VerifyTwoFactorRequest request);
    void resendTwoFactorCode(ResendTwoFactorRequest request);
}
