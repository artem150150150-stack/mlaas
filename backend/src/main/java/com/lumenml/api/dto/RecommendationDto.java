package com.lumenml.api.dto;

import com.lumenml.domain.RecommendationSeverity;
import java.time.Instant;
import java.util.UUID;

public record RecommendationDto(
        UUID id, String code, RecommendationSeverity severity, String message, String detailsJson, Instant createdAt) {}
