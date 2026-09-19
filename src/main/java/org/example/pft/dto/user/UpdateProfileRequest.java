package org.example.pft.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @URL(message = "Invalid avatar URL")
    private String avatar;

    @NotNull(message = "Two factor must be true or false")
    private Boolean twoFactorEnabled;

}
