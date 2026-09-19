package org.example.pft.service;

import org.example.pft.dto.user.ProfileUserUpdateRequest;
import org.example.pft.dto.user.UserResponse;

public interface UserService {
    UserResponse updateProfile(ProfileUserUpdateRequest request);
}
