package org.example.pft.dto.login;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private boolean success = false;
    private String message;
    private LoginData data;
}
