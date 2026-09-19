package org.example.pft.service;

import org.example.pft.dto.user.UpdateProfileRequest;
import org.example.pft.dto.user.UserResponse;

public interface UserService {
    UserResponse updateProfile(UpdateProfileRequest request);
}
