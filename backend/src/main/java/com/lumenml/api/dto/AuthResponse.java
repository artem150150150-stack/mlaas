package com.lumenml.api.dto;

public record AuthResponse(
        String accessToken, String refreshToken, long expiresInSeconds, UserDto user) {}
