package org.example.pft.service;

import org.example.pft.dto.login.LoginRequest;
import org.example.pft.dto.login.LoginResponse;
import org.example.pft.dto.login.RegisterRequest;
import org.example.pft.dto.login.RegisterResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    RegisterResponse register(RegisterRequest request);
}
