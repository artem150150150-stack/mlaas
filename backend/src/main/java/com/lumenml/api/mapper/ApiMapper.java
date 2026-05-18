package com.lumenml.api.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenml.api.dto.DatasetDto;
import com.lumenml.api.dto.ExplainabilityDto;
import com.lumenml.api.dto.ProjectDto;
import com.lumenml.api.dto.RecommendationDto;
import com.lumenml.api.dto.TrainingMetricsDto;
import com.lumenml.api.dto.TrainingTaskDto;
import com.lumenml.api.dto.UserDto;
import com.lumenml.domain.Dataset;
import com.lumenml.domain.ExplainabilityReport;
import com.lumenml.domain.Project;
import com.lumenml.domain.Recommendation;
import com.lumenml.domain.TrainingMetrics;
import com.lumenml.domain.TrainingTask;
import com.lumenml.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiMapper {

    private final ObjectMapper objectMapper;

    public UserDto toUserDto(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getRole());
    }

    public ProjectDto toProjectDto(Project p) {
        return new ProjectDto(
                p.getId(), p.getName(), p.getDescription(), p.getOwner().getId(), p.getCreatedAt());
    }

    public DatasetDto toDatasetDto(Dataset d) {
        return new DatasetDto(
                d.getId(),
                d.getProject().getId(),
                d.getOriginalFilename(),
                d.getTaskType(),
                d.getTargetColumn(),
                d.getRowCount(),
                d.getCreatedAt());
    }

    public TrainingTaskDto toTrainingTaskDto(TrainingTask t, TrainingMetrics m) {
        TrainingMetricsDto md = null;
        if (m != null) {
            List<List<Integer>> cm = null;
            if (m.getConfusionMatrixJson() != null) {
                try {
                    cm = objectMapper.readValue(m.getConfusionMatrixJson(), new TypeReference<>() {});
                } catch (JsonProcessingException ignored) {
                    cm = null;
                }
            }
            md = new TrainingMetricsDto(
                    m.getAccuracy(),
                    m.getPrecisionMacro(),
                    m.getRecallMacro(),
                    m.getF1Macro(),
                    m.getRmse(),
                    cm,
                    m.getTrainScore(),
                    m.getValScore(),
                    m.getOverfittingEstimate());
        }
        return new TrainingTaskDto(
                t.getId(),
                t.getProject().getId(),
                t.getDataset().getId(),
                t.getMlModel() != null ? t.getMlModel().getId() : null,
                t.getModelKind(),
                t.getTaskType(),
                t.getStatus(),
                t.getErrorMessage(),
                t.getStartedAt(),
                t.getFinishedAt(),
                t.getCreatedAt(),
                md);
    }

    public ExplainabilityDto toExplainDto(ExplainabilityReport r) throws JsonProcessingException {
        return new ExplainabilityDto(
                r.getId(),
                r.getTrainingTask().getId(),
                objectMapper.readTree(r.getFeatureImportanceJson()),
                r.getShapValuesJson() != null ? objectMapper.readTree(r.getShapValuesJson()) : null,
                r.getLimeExplanationsJson() != null ? objectMapper.readTree(r.getLimeExplanationsJson()) : null,
                r.getFairnessMetricsJson() != null ? objectMapper.readTree(r.getFairnessMetricsJson()) : null,
                r.getCreatedAt());
    }

    public RecommendationDto toRecoDto(Recommendation r) {
        return new RecommendationDto(
                r.getId(), r.getCode(), r.getSeverity(), r.getMessage(), r.getDetailsJson(), r.getCreatedAt());
    }
}
