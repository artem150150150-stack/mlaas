package com.lumenml.security;

import com.lumenml.domain.UserRole;
import java.security.Principal;
import java.util.UUID;

public record AuthPrincipal(UUID id, String email, UserRole role) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
