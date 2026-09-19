package org.example.pft.controller;

import org.example.pft.dto.user.UpdateProfileRequest;
import org.example.pft.dto.user.UserData;
import org.example.pft.dto.user.UserResponse;
import org.example.pft.security.CustomUserDetailsService;
import org.example.pft.security.JwtAuthenticationFilter;
import org.example.pft.security.JwtService;
import org.example.pft.security.RestAccessDeniedHandler;
import org.example.pft.security.RestAuthenticationEntityPoint;
import org.example.pft.security.SecurityConfig;
import org.example.pft.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntityPoint.class,
        RestAccessDeniedHandler.class
})
class UserControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private UserResponse userResponse;

    @BeforeEach
    void setup() {
        userResponse = new UserResponse(
                true,
                "User profile updated successfully",
                new UserData(
                        1L,
                        "Updated User",
                        "user@example.com",
                        "new-avatar.png",
                        true
                )
        );
    }

    @Test
    void updateProfile_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "fullName": "Updated User",
                    "avatar": "new-avatar.png",
                    "twoFactorEnabled": true
                }
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/user/profile"));

        verify(userService, never()).updateProfile(any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser
    void updateProfile_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(userService.updateProfile(any(UpdateProfileRequest.class)))
                .thenReturn(userResponse);

        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "fullName": "Updated User",
                    "avatar": "new-avatar.png",
                    "twoFactorEnabled": true
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User profile updated successfully"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.fullName").value("Updated User"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.avatar").value("new-avatar.png"))
                .andExpect(jsonPath("$.data.twoFactorEnabled").value(true));

        ArgumentCaptor<UpdateProfileRequest> requestCaptor =
                ArgumentCaptor.forClass(UpdateProfileRequest.class);
        verify(userService).updateProfile(requestCaptor.capture());

        assertEquals("Updated User", requestCaptor.getValue().getFullName());
        assertEquals("new-avatar.png", requestCaptor.getValue().getAvatar());
        assertEquals(true, requestCaptor.getValue().getTwoFactorEnabled());
    }

    @Test
    @WithMockUser
    void updateProfile_withoutAvatar_shouldReturn200() throws Exception {
        UserResponse responseWithoutAvatar = new UserResponse(
                true,
                "User profile updated successfully",
                new UserData(
                        1L,
                        "Updated User",
                        "user@example.com",
                        null,
                        false
                )
        );

        when(userService.updateProfile(any(UpdateProfileRequest.class)))
                .thenReturn(responseWithoutAvatar);

        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "fullName": "Updated User",
                    "twoFactorEnabled": false
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avatar").doesNotExist())
                .andExpect(jsonPath("$.data.twoFactorEnabled").value(false));

        ArgumentCaptor<UpdateProfileRequest> requestCaptor =
                ArgumentCaptor.forClass(UpdateProfileRequest.class);
        verify(userService).updateProfile(requestCaptor.capture());

        assertEquals("Updated User", requestCaptor.getValue().getFullName());
        assertEquals(null, requestCaptor.getValue().getAvatar());
        assertEquals(false, requestCaptor.getValue().getTwoFactorEnabled());
    }

    @Test
    @WithMockUser
    void updateProfile_withoutFullName_shouldReturn422() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "avatar": "new-avatar.png",
                    "twoFactorEnabled": true
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("fullName"))
                .andExpect(jsonPath("$.errors[0].message").value("Full name is required"));

        verify(userService, never()).updateProfile(any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser
    void updateProfile_withoutTwoFactorEnabled_shouldReturn422() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "fullName": "Updated User",
                    "avatar": "new-avatar.png"
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("twoFactorEnabled"))
                .andExpect(jsonPath("$.errors[0].message").value("Two factor must be true or false"));

        verify(userService, never()).updateProfile(any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser
    void updateProfile_withInvalidTwoFactorEnabledType_shouldReturn422() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "fullName": "Updated User",
                    "avatar": "new-avatar.png",
                    "twoFactorEnabled": "abc"
                }
                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("twoFactorEnabled"))
                .andExpect(jsonPath("$.errors[0].message").value("Must be true or false"));

        verify(userService, never()).updateProfile(any(UpdateProfileRequest.class));
    }
}
