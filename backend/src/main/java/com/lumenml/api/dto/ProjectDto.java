package com.lumenml.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectDto(UUID id, String name, String description, UUID ownerId, Instant createdAt) {}
