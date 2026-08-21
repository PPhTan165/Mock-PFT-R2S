package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.login.LoginRequest;
import org.example.pft.dto.login.LoginResponse;
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
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }
}
