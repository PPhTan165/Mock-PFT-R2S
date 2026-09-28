package org.example.pft.security;

import org.example.pft.entity.Role;
import org.example.pft.entity.User;
import org.example.pft.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "encoded-password";

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_withExistingUserAndMultipleRoles_shouldReturnUserDetailsWithRoleAuthorities() {
        User user = userWithRoles(role("USER"), role("ADMIN"));
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

        assertEquals(EMAIL, userDetails.getUsername());
        assertEquals(PASSWORD, userDetails.getPassword());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
        assertEquals(
                Set.of("ROLE_USER", "ROLE_ADMIN"),
                authorityNames(userDetails)
        );
        verify(userRepository).findByEmail(EMAIL);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void loadUserByUsername_withMissingUser_shouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(EMAIL)
        );

        assertEquals("User not found with email: " + EMAIL, exception.getMessage());
        verify(userRepository).findByEmail(EMAIL);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void loadUserByUsername_withEmptyRoles_shouldReturnUserDetailsWithoutAuthorities() {
        User user = userWithRoles();
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

        assertEquals(EMAIL, userDetails.getUsername());
        assertEquals(PASSWORD, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().isEmpty());
        verify(userRepository).findByEmail(EMAIL);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void loadUserByUsername_withNullRoleCollection_shouldReturnUserDetailsWithoutAuthorities() {
        User user = userWithRoles((Set<Role>) null);
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

        assertEquals(EMAIL, userDetails.getUsername());
        assertEquals(PASSWORD, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().isEmpty());
        verify(userRepository).findByEmail(EMAIL);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void loadUserByUsername_withNullRoleName_shouldSkipNullRoleAndKeepNamedAuthorities() {
        User user = userWithRoles(role("USER"), role(null));
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

        assertEquals(
                Set.of("ROLE_USER"),
                authorityNames(userDetails)
        );
        verify(userRepository).findByEmail(EMAIL);
        verifyNoMoreInteractions(userRepository);
    }

    private static User userWithRoles(Role... roles) {
        Set<Role> roleSet = new LinkedHashSet<>();
        Collections.addAll(roleSet, roles);
        return userWithRoles(roleSet);
    }

    private static User userWithRoles(Set<Role> roles) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);
        user.setRoles(roles);
        return user;
    }

    private static Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private static Set<String> authorityNames(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
