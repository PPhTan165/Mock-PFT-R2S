package org.example.pft.dto.twoFactor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyTwoFactorRequest {
    @NotBlank(message = "Challenge ID is required")
    private String challengeId;

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "\\d{6}")
    private String code;
}
