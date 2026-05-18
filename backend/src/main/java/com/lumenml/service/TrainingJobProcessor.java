package com.lumenml.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenml.config.LumenMlProperties;
import com.lumenml.domain.Dataset;
import com.lumenml.domain.ExplainabilityReport;
import com.lumenml.domain.MetricSnapshot;
import com.lumenml.domain.MlModel;
import com.lumenml.domain.TaskStatus;
import com.lumenml.domain.TrainingMetrics;
import com.lumenml.domain.TrainingTask;
import com.lumenml.ml.MlTrainResult;
import com.lumenml.ml.MlTrainingEngine;
import com.lumenml.ml.RecommendationEngine;
import com.lumenml.rabbit.MetricsEvent;
import com.lumenml.rabbit.NotificationEvent;
import com.lumenml.rabbit.TrainingRabbitConfig;
import com.lumenml.repository.ExplainabilityReportRepository;
import com.lumenml.repository.MetricSnapshotRepository;
import com.lumenml.repository.MlModelRepository;
import com.lumenml.repository.RecommendationRepository;
import com.lumenml.repository.TrainingMetricsRepository;
import com.lumenml.repository.TrainingTaskRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingJobProcessor {

    private final TrainingTaskRepository trainingTaskRepository;
    private final TrainingMetricsRepository trainingMetricsRepository;
    private final ExplainabilityReportRepository explainabilityReportRepository;
    private final RecommendationRepository recommendationRepository;
    private final MetricSnapshotRepository metricSnapshotRepository;
    private final MlModelRepository mlModelRepository;
    private final MlTrainingEngine mlTrainingEngine;
    private final RecommendationEngine recommendationEngine;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final LumenMlProperties props;

    public void handle(UUID taskId) throws Exception {
        TrainingTask task = trainingTaskRepository.findDetailedById(taskId).orElseThrow();
        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(Instant.now());
        task.setErrorMessage(null);
        trainingTaskRepository.save(task);

        Dataset dataset = task.getDataset();
        Path csv = Path.of(dataset.getStorageUri());
        List<String> features =
                objectMapper.readValue(dataset.getFeatureColumnsJson(), new TypeReference<>() {});
        Map<String, Object> hp =
                objectMapper.readValue(task.getHyperparametersJson(), new TypeReference<>() {});

        MlTrainResult result = mlTrainingEngine.train(
                csv, task.getTaskType(), task.getModelKind(), dataset.getTargetColumn(), features, hp);

        TrainingMetrics metrics = TrainingMetrics.builder()
                .trainingTask(task)
                .accuracy(result.getAccuracy())
                .precisionMacro(result.getPrecisionMacro())
                .recallMacro(result.getRecallMacro())
                .f1Macro(result.getF1Macro())
                .rmse(result.getRmse())
                .confusionMatrixJson(result.getConfusionMatrix() == null
                        ? null
                        : objectMapper.writeValueAsString(result.getConfusionMatrix()))
                .trainScore(result.getTrainScore())
                .valScore(result.getValScore())
                .overfittingEstimate(result.getOverfittingEstimate())
                .build();
        trainingMetricsRepository.save(metrics);

        ExplainabilityReport explain = ExplainabilityReport.builder()
                .trainingTask(task)
                .featureImportanceJson(objectMapper.writeValueAsString(result.getFeatureImportance()))
                .shapValuesJson(objectMapper.writeValueAsString(result.getShapSummary()))
                .limeExplanationsJson(objectMapper.writeValueAsString(result.getLimeSamples()))
                .fairnessMetricsJson("{\"note\":\"Provide sensitive attributes to enable fairness metrics\"}")
                .build();
        explainabilityReportRepository.save(explain);

        recommendationRepository.saveAll(
                recommendationEngine.build(task, result, dataset.getColumnStatsJson()));

        snapshot(task, "primary_score", primaryScore(result));
        snapshot(task, "drift_hint", 0.02);

        task.setStatus(TaskStatus.SUCCEEDED);
        task.setFinishedAt(Instant.now());
        trainingTaskRepository.save(task);

        if (task.getMlModel() != null) {
            MlModel model = task.getMlModel();
            model.setLatestTrainingTaskId(task.getId());
            model.setArtifactUri("inline-smile-model:" + task.getId());
            mlModelRepository.save(model);
        }

        rabbitTemplate.convertAndSend(
                props.getRabbit().getNotificationExchange(),
                "notification.created",
                new NotificationEvent("TASK_COMPLETED", task.getId(), "Training finished"));
        rabbitTemplate.convertAndSend(
                props.getRabbit().getMetricsExchange(),
                "metrics.process",
                new MetricsEvent(task.getId(), "train_score", result.getTrainScore()));
    }

    private void snapshot(TrainingTask task, String name, double value) {
        metricSnapshotRepository.save(MetricSnapshot.builder()
                .trainingTask(task)
                .metricName(name)
                .metricValue(value)
                .driftScore(null)
                .simulated(false)
                .build());
    }

    private static double primaryScore(MlTrainResult r) {
        if (r.getAccuracy() != null) {
            return r.getAccuracy();
        }
        if (r.getRmse() != null) {
            return r.getRmse();
        }
        return 0;
    }
}
