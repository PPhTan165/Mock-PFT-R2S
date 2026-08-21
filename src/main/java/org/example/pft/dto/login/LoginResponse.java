package org.example.pft.dto.login;

import lombok.Getter;

@Getter
public class LoginResponse {
    private String accessToken;
    private long expire;
}
