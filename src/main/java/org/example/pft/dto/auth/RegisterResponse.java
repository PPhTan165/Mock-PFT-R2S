package org.example.pft.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {
    private boolean success = false;
    private String message;
    private RegisterData data;
}
