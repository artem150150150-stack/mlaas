package com.lumenml.api.dto;

import com.lumenml.domain.ModelKind;
import com.lumenml.domain.TaskStatus;
import com.lumenml.domain.TaskType;
import java.time.Instant;
import java.util.UUID;

public record TrainingTaskDto(
        UUID id,
        UUID projectId,
        UUID datasetId,
        UUID mlModelId,
        ModelKind modelKind,
        TaskType taskType,
        TaskStatus status,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        TrainingMetricsDto metrics) {}
