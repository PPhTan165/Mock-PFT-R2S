package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.auth.LoginRequest;
import org.example.pft.dto.auth.LoginResponse;
import org.example.pft.dto.auth.RegisterRequest;
import org.example.pft.dto.auth.RegisterResponse;
import org.example.pft.dto.twoFactor.ResendTwoFactorRequest;
import org.example.pft.dto.twoFactor.VerifyTwoFactorRequest;
import org.example.pft.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResponse> verifyTwoFactor(
            @Valid @RequestBody VerifyTwoFactorRequest request
    ) {
        return ResponseEntity.ok(
                authService.verifyTwoFactor(request)
        );
    }

    @PostMapping("/2fa/resend")
    public ResponseEntity<Void> resendTwoFactorCode(
            @Valid @RequestBody ResendTwoFactorRequest request
    ) {
        authService.resendTwoFactorCode(request);
        return ResponseEntity.noContent().build();
    }
}
