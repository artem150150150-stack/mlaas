package com.lumenml.ml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenml.domain.Recommendation;
import com.lumenml.domain.RecommendationSeverity;
import com.lumenml.domain.TrainingTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final ObjectMapper objectMapper;

    public List<Recommendation> build(TrainingTask task, MlTrainResult result, String columnStatsJson) {
        List<Recommendation> list = new ArrayList<>();
        if (result.getOverfittingEstimate() != null
                && result.getOverfittingEstimate() > 0.08d) {
            list.add(rec(
                    task,
                    "OVERFITTING",
                    RecommendationSeverity.WARN,
                    "Model is overfitting (large gap between train and validation scores)",
                    Map.of("estimate", result.getOverfittingEstimate())));
        }
        if (result.getFeatureImportance() != null) {
            result.getFeatureImportance().entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue() > 0.35)
                    .findFirst()
                    .ifPresent(e -> list.add(rec(
                            task,
                            "DOMINANT_FEATURE",
                            RecommendationSeverity.INFO,
                            "Feature %s strongly impacts prediction — review for leakage or bias"
                                    .formatted(e.getKey()),
                            Map.of("feature", e.getKey(), "importance", e.getValue()))));
        }
        if (columnStatsJson != null && !columnStatsJson.isBlank()) {
            try {
                Map<String, Map<String, Object>> stats =
                        objectMapper.readValue(columnStatsJson, new TypeReference<>() {});
                stats.forEach((col, m) -> {
                    Object miss = m.get("missingRatio");
                    if (miss instanceof Number n && n.doubleValue() > 0.05) {
                        list.add(rec(
                                task,
                                "MISSING_VALUES",
                                RecommendationSeverity.WARN,
                                "Too many missing values in column " + col,
                                Map.of("column", col, "missingRatio", n.doubleValue())));
                    }
                });
            } catch (Exception ignored) {
                // ignore malformed stats
            }
        }
        addImbalanceFromConfusionMatrix(task, result, list);
        list.add(rec(
                task,
                "IMPROVE_GENERALIZATION",
                RecommendationSeverity.INFO,
                "Try reducing max_depth or n_estimators if validation metrics lag",
                Map.of()));
        return list;
    }

    private void addImbalanceFromConfusionMatrix(TrainingTask task, MlTrainResult result, List<Recommendation> list) {
        if (result.getConfusionMatrix() == null || result.getConfusionMatrix().isEmpty()) {
            return;
        }
        List<List<Integer>> cm = result.getConfusionMatrix();
        int n = cm.size();
        if (n < 2) {
            return;
        }
        int[] support = new int[n];
        int total = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int v = cm.get(i).get(j);
                total += v;
                support[i] += v;
            }
        }
        if (total == 0) {
            return;
        }
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int s : support) {
            min = Math.min(min, s);
            max = Math.max(max, s);
        }
        if (max > 0 && (double) min / max < 0.2) {
            list.add(rec(
                    task,
                    "CLASS_IMBALANCE",
                    RecommendationSeverity.WARN,
                    "Dataset has class imbalance — consider SMOTE or class weights",
                    Map.of("minSupport", min, "maxSupport", max)));
        }
    }

    private Recommendation rec(
            TrainingTask task,
            String code,
            RecommendationSeverity severity,
            String message,
            Map<String, Object> details) {
        String json = null;
        try {
            if (!details.isEmpty()) {
                json = objectMapper.writeValueAsString(details);
            }
        } catch (Exception ignored) {
            json = null;
        }
        return Recommendation.builder()
                .trainingTask(task)
                .code(code)
                .severity(severity)
                .message(message)
                .detailsJson(json)
                .build();
    }
}
