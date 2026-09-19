package org.example.pft.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserData {
    private Long userId;
    private String fullName;
    private String email;
    private String avatar;
    private Boolean twoFactorEnabled;
}
