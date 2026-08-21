package org.example.pft.service.impl;

import org.example.pft.dto.login.LoginRequest;
import org.example.pft.dto.login.LoginResponse;
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
        long exp = jwtService.getExpirationSeconds(accessToken);

        return new LoginResponse(accessToken,exp);

    }
}
