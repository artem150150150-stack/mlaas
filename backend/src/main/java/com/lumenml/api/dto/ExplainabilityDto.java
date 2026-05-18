package com.lumenml.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ExplainabilityDto(
        UUID id,
        UUID trainingTaskId,
        JsonNode featureImportance,
        JsonNode shapValues,
        JsonNode limeExplanations,
        JsonNode fairnessMetrics,
        Instant createdAt) {}
