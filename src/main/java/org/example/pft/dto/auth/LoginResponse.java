package org.example.pft.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"success","message","challengeId","data"})
public class LoginResponse {
    private boolean success;
    private String message;
    private String challengeId;
    private LoginData data;

    public LoginResponse(boolean success, String message, LoginData data) {
        this.success = success;
        this.message = message;
        this.challengeId = null;
        this.data = data;
    }
}
