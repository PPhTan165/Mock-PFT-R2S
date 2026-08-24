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

import java.time.LocalDateTime;
import java.util.Set;

@Service

public class AuthServiceImpl implements AuthService {
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

        if(!encoder.matches(request.getPassword(),user.getPassword())){
            throw new BusinessValidationException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user);
        LocalDateTime exp = jwtService.getExpirationDateTime(accessToken);

        LoginData data = new LoginData(accessToken,exp);

        return new LoginResponse(true,"Login successful",data);

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
