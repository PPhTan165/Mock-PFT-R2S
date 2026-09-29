package org.example.pft.dto.twoFactor;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendTwoFactorRequest {

    @NotBlank(message = "Challenge ID is required")
    private String challengeId;
}