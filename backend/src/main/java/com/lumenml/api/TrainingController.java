package com.lumenml.api;

import com.lumenml.api.dto.ExplainabilityDto;
import com.lumenml.api.dto.RecommendationDto;
import com.lumenml.api.dto.TrainingStartRequest;
import com.lumenml.api.dto.TrainingTaskDto;
import com.lumenml.domain.TaskStatus;
import com.lumenml.security.SecurityUtils;
import com.lumenml.service.TrainingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping("/projects/{projectId}/training")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TrainingTaskDto start(
            @PathVariable UUID projectId, @Valid @RequestBody TrainingStartRequest request) throws Exception {
        return trainingService.start(SecurityUtils.requireCurrentUser(), projectId, request);
    }

    @GetMapping("/projects/{projectId}/training/tasks")
    public Page<TrainingTaskDto> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            Pageable pageable) {
        return trainingService.list(SecurityUtils.requireCurrentUser(), projectId, status, pageable);
    }

    @GetMapping("/training/tasks/{taskId}")
    public TrainingTaskDto get(@PathVariable UUID taskId) {
        return trainingService.get(SecurityUtils.requireCurrentUser(), taskId);
    }

    @GetMapping("/training/tasks/{taskId}/explain")
    public ExplainabilityDto explain(@PathVariable UUID taskId) throws Exception {
        return trainingService.explain(SecurityUtils.requireCurrentUser(), taskId);
    }

    @GetMapping("/training/tasks/{taskId}/recommendations")
    public List<RecommendationDto> recommendations(@PathVariable UUID taskId) {
        return trainingService.recommendations(SecurityUtils.requireCurrentUser(), taskId);
    }
}
