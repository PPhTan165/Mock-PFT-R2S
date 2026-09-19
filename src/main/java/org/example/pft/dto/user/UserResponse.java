package org.example.pft.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UserResponse {
    private boolean success;
    private String message;
    private UserData data;
}
