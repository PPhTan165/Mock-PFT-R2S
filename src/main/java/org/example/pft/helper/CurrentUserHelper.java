package org.example.pft.helper;

import lombok.AllArgsConstructor;
import org.example.pft.entity.User;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CurrentUserHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentUserHelper.class);
    private static final String ANONYMOUS_USER = "anonymousUser";

    private final UserRepository userRepository;

    public Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }

        return authentication;
    }

    public String getCurrentEmailUser() {
        String email = getAuthentication().getName();
        if (email == null || ANONYMOUS_USER.equals(email)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }

        LOGGER.debug("Resolved current authenticated principal");
        return email;
    }

    public User getCurrentUser(){
        return userRepository.findByEmail(getCurrentEmailUser())
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
    }
}
