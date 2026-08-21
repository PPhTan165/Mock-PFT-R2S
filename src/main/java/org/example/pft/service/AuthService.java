package org.example.pft.service;

import org.example.pft.dto.login.LoginRequest;
import org.example.pft.dto.login.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
