package org.example.pft.controller;

import org.example.pft.dto.auth.RegisterData;
import org.example.pft.dto.auth.RegisterRequest;
import org.example.pft.dto.auth.RegisterResponse;
import org.example.pft.exception.BusinessConflictException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
public class RegisterControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private RegisterResponse registerResponse;

    @BeforeEach
    void setup() {
        RegisterData registerData = new RegisterData(
                1L,
                "Test User",
                "user@gmail.com"
        );
        registerResponse = new RegisterResponse(
                true,
                "Registration successful",
                registerData
        );
    }

    @Test
    void register_withoutToken_shouldReturn200() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(registerResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "user@gmail.com",
                    "password": "password123",
                    "fullName": "Test User"
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.email").value("user@gmail.com"));
    }

    @Test
    void register_withExistingEmail_shouldReturn409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessConflictException("Email is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "user@gmail.com",
                    "password": "password123",
                    "fullName": "Test User"
                }
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_withoutEmail_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "password": "password123",
                    "fullName": "Test User"
                }
                """))
                .andExpect(status().isUnprocessableContent());

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void register_withInvalidEmail_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "invalid-email",
                    "password": "password123",
                    "fullName": "Test User"
                }
                """))
                .andExpect(status().isUnprocessableContent());

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void register_withBlankFullName_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "user@gmail.com",
                    "password": "password123",
                    "fullName": ""
                }
                """))
                .andExpect(status().isUnprocessableContent());

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void register_withPasswordTooShort_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "user@gmail.com",
                    "password": "pass1",
                    "fullName": "Test User"
                }
                """))
                .andExpect(status().isUnprocessableContent());

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void register_withPasswordWithoutNumber_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "user@gmail.com",
                    "password": "password",
                    "fullName": "Test User"
                }
                """))
                .andExpect(status().isUnprocessableContent());

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }

    @Test
    void register_withPasswordWithoutLetter_shouldReturn422() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "user@gmail.com",
                    "password": "12345678",
                    "fullName": "Test User"
                }
                """))
                .andExpect(status().isUnprocessableContent());

        verify(authService, never())
                .register(any(RegisterRequest.class));
    }
}
