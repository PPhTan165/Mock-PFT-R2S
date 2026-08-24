package org.example.pft.helper;

import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.example.pft.entity.User;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CurrentUserHelper {
    private final UserRepository userRepository;

    public Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()){
            throw new RuntimeException("Unauthenticated");
        }

        return authentication;
    }

    public String getCurrentEmailUser() {
        String email = getAuthentication().getName();
        System.out.println(email);
        if("anonymousUser".equals(email)){
            throw new RuntimeException("Unauthenticated");
        }

        return email;
    }

    public User getCurrentUser(){
        return userRepository.findByEmail(getCurrentEmailUser())
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
    }
}
