package com.lumenml.api.dto;

import java.util.List;

public record TrainingMetricsDto(
        Double accuracy,
        Double precisionMacro,
        Double recallMacro,
        Double f1Macro,
        Double rmse,
        List<List<Integer>> confusionMatrix,
        Double trainScore,
        Double valScore,
        Double overfittingEstimate) {}
