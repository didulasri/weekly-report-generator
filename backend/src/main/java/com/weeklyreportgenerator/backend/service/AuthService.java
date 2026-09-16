package com.weeklyreportgenerator.backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.dto.request.LoginRequest;
import com.weeklyreportgenerator.backend.dto.request.RegisterRequest;
import com.weeklyreportgenerator.backend.dto.response.AuthResponse;
import com.weeklyreportgenerator.backend.dto.response.UserSummaryResponse;
import com.weeklyreportgenerator.backend.entity.Role;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;
import com.weeklyreportgenerator.backend.exception.DuplicateResourceException;
import com.weeklyreportgenerator.backend.repository.RoleRepository;
import com.weeklyreportgenerator.backend.repository.UserRepository;
import com.weeklyreportgenerator.backend.security.CustomUserDetails;
import com.weeklyreportgenerator.backend.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        Role teamMemberRole = roleRepository.findByName(RoleName.TEAM_MEMBER)
                .orElseThrow(() -> new IllegalStateException("TEAM_MEMBER role is not seeded"));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(teamMemberRole)
                .active(true)
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(toSummary(userDetails))
                .build();
    }

    public UserSummaryResponse getCurrentUser(CustomUserDetails userDetails) {
        return toSummary(userDetails);
    }

    private UserSummaryResponse toSummary(CustomUserDetails userDetails) {
        return UserSummaryResponse.builder()
                .id(userDetails.getId())
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .role(userDetails.getRole())
                .build();
    }
}
