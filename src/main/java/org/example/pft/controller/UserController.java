package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.user.UpdateProfileRequest;
import org.example.pft.dto.user.UserResponse;
import org.example.pft.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestBody @Valid UpdateProfileRequest request){
        return ResponseEntity.ok().body(userService.updateProfile(request));
    }
}
