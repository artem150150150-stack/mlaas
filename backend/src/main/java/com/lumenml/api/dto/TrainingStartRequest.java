package com.lumenml.api.dto;

import com.lumenml.domain.ModelKind;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record TrainingStartRequest(
        @NotNull UUID datasetId,
        @NotNull ModelKind modelKind,
        String modelName,
        Map<String, Object> hyperparameters) {}
