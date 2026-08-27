package org.example.pft.service;

import org.example.pft.dto.auth.LoginRequest;
import org.example.pft.dto.auth.LoginResponse;
import org.example.pft.dto.auth.RegisterRequest;
import org.example.pft.dto.auth.RegisterResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    RegisterResponse register(RegisterRequest request);
}
