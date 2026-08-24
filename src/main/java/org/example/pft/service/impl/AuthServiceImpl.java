package org.example.pft.service.impl;

import org.example.pft.dto.login.*;
import org.example.pft.entity.Role;
import org.example.pft.entity.User;
import org.example.pft.exception.BusinessConflictException;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.repository.RoleRepository;
import org.example.pft.repository.UserRepository;
import org.example.pft.security.JwtService;
import org.example.pft.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service

public class AuthServiceImpl implements AuthService {
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_LOCK_MINUTES = 30;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder encoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new BusinessValidationException("Invalid email or password"));

        checkLoginLock(user);

        if(!encoder.matches(request.getPassword(),user.getPassword())){
            throw handleFailedLogin(user);
        }

        resetLoginFailure(user);

        String accessToken = jwtService.generateToken(user);
        LocalDateTime exp = jwtService.getExpirationDateTime(accessToken);

        LoginData data = new LoginData(accessToken,exp);

        return new LoginResponse(true,"Login successful",data);

    }

    private void checkLoginLock(User user) {
        if (user.getLockedUntil() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(user.getLockedUntil())) {
            long minutesLeft = Duration.between(now, user.getLockedUntil()).toMinutes() + 1;
            throw new BusinessValidationException(
                    "Account is locked. Try again after " + minutesLeft + " minutes"
            );
        }

        resetLoginFailure(user);
    }

    private BusinessValidationException handleFailedLogin(User user) {
        int failedLoginAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(failedLoginAttempts);
        System.out.println(failedLoginAttempts);
        if (failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOGIN_LOCK_MINUTES));
            userRepository.save(user);
            return new BusinessValidationException(
                    "Account is locked. Try again after " + LOGIN_LOCK_MINUTES + " minutes"
            );
        }

        userRepository.save(user);
        return new BusinessValidationException("Invalid email or password");
    }

    private void resetLoginFailure(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    @Override
    public RegisterResponse register(RegisterRequest request){

        if(userRepository.existsByEmail(request.getEmail())){
            throw new BusinessConflictException("Email is already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(()-> new ResourceNotFoundException("Default role USER not found"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());

        user.setPassword(encoder.encode(request.getPassword()));
        user.setRoles(Set.of(userRole));

       User savedUser = userRepository.save(user);

        RegisterData data = new RegisterData(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail()
        );

        return new RegisterResponse(true,"Registration successful",data);
    }
}
