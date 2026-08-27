package org.example.pft.dto.login;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterData {
    private Long userId;
    private String fullName;
    private String email;
}
