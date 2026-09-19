package org.example.pft.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfileUserUpdateRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    private String avatar;

    @NotNull(message = "Two factor must be true or false")
    private Boolean twoFactorEnabled;

}
