package com.lumenml.api.dto;

import com.lumenml.domain.TaskType;
import java.time.Instant;
import java.util.UUID;

public record DatasetDto(
        UUID id,
        UUID projectId,
        String originalFilename,
        TaskType taskType,
        String targetColumn,
        Long rowCount,
        Instant createdAt) {}
