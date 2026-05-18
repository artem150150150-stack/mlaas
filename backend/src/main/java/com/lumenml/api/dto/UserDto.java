package com.lumenml.api.dto;

import com.lumenml.domain.UserRole;
import java.util.UUID;

public record UserDto(UUID id, String email, UserRole role) {}
