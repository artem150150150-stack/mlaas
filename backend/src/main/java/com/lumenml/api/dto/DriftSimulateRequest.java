package com.lumenml.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DriftSimulateRequest(@NotNull UUID trainingTaskId, double driftScore, boolean simulated) {}
