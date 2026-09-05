package org.example.pft.service;

import org.example.pft.dto.auth.RegisterRequest;
import org.example.pft.dto.auth.RegisterResponse;
import org.example.pft.entity.Role;
import org.example.pft.entity.User;
import org.example.pft.exception.BusinessConflictException;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.repository.RoleRepository;
import org.example.pft.repository.UserRepository;
import org.example.pft.security.JwtService;
import org.example.pft.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RegisterServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthServiceImpl authService;

    private RegisterRequest request;
    private Role userRole;

    @BeforeEach
    void setup() {
        request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");
    }

    @Test
    void register_withValidRequest_shouldCreateUser() {
        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    savedUser.setId(1L);
                    return savedUser;
                });

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Registration successful", response.getMessage());
        assertEquals(1L, response.getData().getUserId());
        assertEquals("Test User", response.getData().getFullName());
        assertEquals("user@example.com", response.getData().getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User userToSave = userCaptor.getValue();
        assertEquals("user@example.com", userToSave.getEmail());
        assertEquals("Test User", userToSave.getFullName());
        assertEquals("encoded-password", userToSave.getPassword());
        assertTrue(userToSave.getRoles().contains(userRole));

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_withExistingEmail_shouldThrowException() {
        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(true);

        assertThrows(
                BusinessConflictException.class,
                () -> authService.register(request)
        );

        verify(roleRepository, never()).findByName("USER");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenDefaultRoleNotFound_shouldThrowException() {
        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> authService.register(request)
        );

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }
}
