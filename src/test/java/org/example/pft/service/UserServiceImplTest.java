package org.example.pft.service;

import org.example.pft.dto.user.UpdateProfileRequest;
import org.example.pft.dto.user.UserData;
import org.example.pft.dto.user.UserResponse;
import org.example.pft.entity.User;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.UserRepository;
import org.example.pft.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    UserRepository userRepository;

    @Mock
    CurrentUserHelper currentUserHelper;

    @InjectMocks
    UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setup(){
        user  = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setFullName("User1");
        user.setPassword("mock-password-example");
        user.setAvatar("old-avatar.png");
        user.setTwoFactorEnabled(false);
    }

    @Test
    void updateProfile_withValidRequest_shouldSaveUserAndReturnResponse() {
        UpdateProfileRequest request = createRequest(
                "Updated User",
                "new-avatar.png",
                true
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);

        UserResponse response = userService.updateProfile(request);

        assertTrue(response.isSuccess());
        assertEquals("User profile updated successfully", response.getMessage());

        UserData data = response.getData();
        assertEquals(1L, data.getUserId());
        assertEquals("Updated User", data.getFullName());
        assertEquals("user@example.com", data.getEmail());
        assertEquals("new-avatar.png", data.getAvatar());
        assertEquals(true, data.getTwoFactorEnabled());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("Updated User", savedUser.getFullName());
        assertEquals("new-avatar.png", savedUser.getAvatar());
        assertEquals(true, savedUser.getTwoFactorEnabled());
    }

    @Test
    void updateProfile_withNullAvatar_shouldClearAvatar() {
        UpdateProfileRequest request = createRequest(
                "Updated User",
                null,
                true
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);

        UserResponse response = userService.updateProfile(request);

        assertTrue(response.isSuccess());
        assertNull(response.getData().getAvatar());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getAvatar());
    }

    @Test
    void updateProfile_withEmptyAvatar_shouldClearAvatar() {
        UpdateProfileRequest request = createRequest(
                "Updated User",
                "",
                false
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);

        UserResponse response = userService.updateProfile(request);

        assertTrue(response.isSuccess());
        assertNull(response.getData().getAvatar());
        assertEquals(false, response.getData().getTwoFactorEnabled());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getAvatar());
        assertEquals(false, userCaptor.getValue().getTwoFactorEnabled());
    }

    private UpdateProfileRequest createRequest(
            String fullName,
            String avatar,
            Boolean twoFactorEnabled) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName(fullName);
        request.setAvatar(avatar);
        request.setTwoFactorEnabled(twoFactorEnabled);
        return request;
    }
}
