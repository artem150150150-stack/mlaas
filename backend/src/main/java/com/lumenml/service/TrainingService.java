package com.lumenml.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenml.api.dto.ExplainabilityDto;
import com.lumenml.api.dto.RecommendationDto;
import com.lumenml.api.dto.TrainingStartRequest;
import com.lumenml.api.dto.TrainingTaskDto;
import com.lumenml.api.mapper.ApiMapper;
import com.lumenml.domain.ModelKind;
import com.lumenml.domain.MlModel;
import com.lumenml.domain.TaskStatus;
import com.lumenml.domain.TaskType;
import com.lumenml.domain.TrainingTask;
import com.lumenml.exception.NotFoundException;
import com.lumenml.rabbit.TrainingJobProducer;
import com.lumenml.repository.ExplainabilityReportRepository;
import com.lumenml.repository.MlModelRepository;
import com.lumenml.repository.RecommendationRepository;
import com.lumenml.repository.TrainingMetricsRepository;
import com.lumenml.repository.TrainingTaskRepository;
import com.lumenml.security.AuthPrincipal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingTaskRepository trainingTaskRepository;
    private final TrainingMetricsRepository trainingMetricsRepository;
    private final ExplainabilityReportRepository explainabilityReportRepository;
    private final RecommendationRepository recommendationRepository;
    private final MlModelRepository mlModelRepository;
    private final TrainingJobProducer trainingJobProducer;
    private final ProjectAccessService projectAccessService;
    private final DatasetService datasetService;
    private final ObjectMapper objectMapper;
    private final ApiMapper apiMapper;

    @Transactional
    public TrainingTaskDto start(AuthPrincipal user, UUID projectId, TrainingStartRequest req) throws Exception {
        projectAccessService.requireForUser(projectId, user);
        var dataset = datasetService.requireInProject(projectId, req.datasetId());
        validateModel(dataset.getTaskType(), req.modelKind());

        String modelName = req.modelName() == null || req.modelName().isBlank()
                ? "model-" + UUID.randomUUID().toString().substring(0, 8)
                : req.modelName().trim();

        MlModel model = MlModel.builder()
                .project(dataset.getProject())
                .dataset(dataset)
                .name(modelName)
                .modelKind(req.modelKind())
                .build();
        mlModelRepository.save(model);

        Map<String, Object> hp = req.hyperparameters() == null ? Map.of() : req.hyperparameters();
        TrainingTask task = TrainingTask.builder()
                .project(dataset.getProject())
                .dataset(dataset)
                .mlModel(model)
                .modelKind(req.modelKind())
                .taskType(dataset.getTaskType())
                .status(TaskStatus.QUEUED)
                .hyperparametersJson(objectMapper.writeValueAsString(hp))
                .build();
        trainingTaskRepository.save(task);

        trainingJobProducer.enqueue(task.getId());
        return apiMapper.toTrainingTaskDto(task, null);
    }

    private void validateModel(TaskType taskType, ModelKind modelKind) {
        if (taskType == TaskType.CLASSIFICATION && modelKind == ModelKind.LINEAR_REGRESSION) {
            throw new IllegalArgumentException("Linear regression cannot be used for classification");
        }
        if (taskType == TaskType.REGRESSION && modelKind == ModelKind.LOGISTIC_REGRESSION) {
            throw new IllegalArgumentException("Logistic regression cannot be used for regression");
        }
    }

    @Transactional(readOnly = true)
    public Page<TrainingTaskDto> list(AuthPrincipal user, UUID projectId, TaskStatus status, Pageable pageable) {
        projectAccessService.requireForUser(projectId, user);
        Page<TrainingTask> page;
        if (status == null) {
            page = trainingTaskRepository.findByProjectId(projectId, pageable);
        } else {
            page = trainingTaskRepository.findByProjectIdAndStatus(projectId, status, pageable);
        }
        return page.map(t -> apiMapper.toTrainingTaskDto(
                t, trainingMetricsRepository.findByTrainingTaskId(t.getId()).orElse(null)));
    }

    @Transactional(readOnly = true)
    public TrainingTaskDto get(AuthPrincipal user, UUID taskId) {
        TrainingTask task = trainingTaskRepository
                .findDetailedById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        projectAccessService.requireForUser(task.getProject().getId(), user);
        var metrics = trainingMetricsRepository.findByTrainingTaskId(taskId).orElse(null);
        return apiMapper.toTrainingTaskDto(task, metrics);
    }

    @Transactional(readOnly = true)
    public ExplainabilityDto explain(AuthPrincipal user, UUID taskId) throws Exception {
        TrainingTask task = trainingTaskRepository
                .findDetailedById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        projectAccessService.requireForUser(task.getProject().getId(), user);
        var report = explainabilityReportRepository
                .findByTrainingTaskId(taskId)
                .orElseThrow(() -> new NotFoundException("Explainability not ready"));
        return apiMapper.toExplainDto(report);
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> recommendations(AuthPrincipal user, UUID taskId) {
        TrainingTask task = trainingTaskRepository
                .findDetailedById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        projectAccessService.requireForUser(task.getProject().getId(), user);
        return recommendationRepository.findByTrainingTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(apiMapper::toRecoDto)
                .toList();
    }
}
