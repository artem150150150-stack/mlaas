package com.lumenml.service;

import com.lumenml.api.dto.AuthResponse;
import com.lumenml.api.dto.LoginRequest;
import com.lumenml.api.dto.RefreshRequest;
import com.lumenml.api.dto.RegisterRequest;
import com.lumenml.api.mapper.ApiMapper;
import com.lumenml.config.LumenMlProperties;
import com.lumenml.domain.User;
import com.lumenml.domain.UserRole;
import com.lumenml.repository.UserRepository;
import com.lumenml.security.AuthPrincipal;
import com.lumenml.security.JwtService;
import com.lumenml.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LumenMlProperties props;
    private final ApiMapper apiMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .enabled(true)
                .build();
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid credentials", ex);
        }
        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        User user;
        try {
            user = refreshTokenService.consumeAndValidate(request.refreshToken());
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid refresh token", ex);
        }
        String access = jwtService.createAccessToken(user);
        String newRefresh = refreshTokenService.issue(user);
        return new AuthResponse(
                access, newRefresh, props.getJwt().getAccessExpirationMs() / 1000, apiMapper.toUserDto(user));
    }

    @Transactional
    public void logout(AuthPrincipal principal) {
        User user = userRepository.getReferenceById(principal.id());
        refreshTokenService.revokeAllForUser(user);
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.createAccessToken(user);
        String refresh = refreshTokenService.issue(user);
        return new AuthResponse(
                access, refresh, props.getJwt().getAccessExpirationMs() / 1000, apiMapper.toUserDto(user));
    }
}
