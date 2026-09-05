package org.example.pft.controller;

import org.example.pft.dto.auth.LoginData;
import org.example.pft.dto.auth.LoginRequest;
import org.example.pft.dto.auth.LoginResponse;
import org.example.pft.security.CustomUserDetailsService;
import org.example.pft.security.JwtAuthenticationFilter;
import org.example.pft.security.JwtService;
import org.example.pft.security.RestAccessDeniedHandler;
import org.example.pft.security.RestAuthenticationEntityPoint;
import org.example.pft.security.SecurityConfig;
import org.example.pft.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntityPoint.class,
        RestAccessDeniedHandler.class
})
class LoginControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private LoginResponse loginResponse;
    private LoginRequest request;

    @BeforeEach
    void setup(){
        request = new LoginRequest();
        request.setEmail("user@gmail.com");
        request.setPassword("123456");

        LoginData loginData = new LoginData(
                "mock-jwt-token",
                LocalDateTime.of(2026, 9, 5, 16, 30)
        );
        loginResponse = new LoginResponse(
                true,
                "Login successful",
                loginData
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(loginResponse);
    }


    @Test
    void login_withoutToken_shouldReturn200() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "user@gmail.com",
                    "password": "123456"
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.expired").exists());
    }

    @Test
    void login_withoutEmail_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "password": "123456"
                }
                """))
                .andExpect(status().isUnprocessableContent());

        verify(authService, never())
                .login(any(LoginRequest.class));
    }

    @Test
    void login_withBlankPassword_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "user@gmail.com",
                    "password": ""
                }
                """))
                .andExpect(status().isUnprocessableContent());

        verify(authService, never())
                .login(any(LoginRequest.class));
    }
}
