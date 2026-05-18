package com.lumenml.rabbit;

import java.util.UUID;

public record MetricsEvent(UUID taskId, String metricName, double value) {}
