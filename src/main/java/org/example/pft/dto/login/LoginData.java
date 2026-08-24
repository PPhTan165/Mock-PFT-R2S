package org.example.pft.dto.login;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LoginData {
    private String accessToken;
    private LocalDateTime expired;
}
