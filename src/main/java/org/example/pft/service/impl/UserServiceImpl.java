package org.example.pft.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.example.pft.dto.user.ProfileUserUpdateRequest;
import org.example.pft.dto.user.UserData;
import org.example.pft.dto.user.UserResponse;
import org.example.pft.entity.User;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.UserRepository;
import org.example.pft.service.UserService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final CurrentUserHelper helper;

    @Transactional
    @Override
    public UserResponse updateProfile(ProfileUserUpdateRequest request) {
        User user = helper.getCurrentUser();

        user.setFullName(request.getFullName());
        if(request.getAvatar() == null || request.getAvatar().isEmpty()){
            user.setAvatar(null);
        }else{
            user.setAvatar(request.getAvatar());
        }
        user.setTwoFactorEnabled(request.getTwoFactorEnabled());

        userRepository.save(user);

        return new UserResponse(
                true,
                "User profile updated successfully",
                mapToData(user)
        );
    }

    private UserData mapToData(User user){
        return new UserData(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatar(),
                user.getTwoFactorEnabled()
        );
    }
}
