package org.example.pft.dto.login;
import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "Email format is invalid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
