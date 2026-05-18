package com.lumenml.api;

import com.lumenml.api.dto.ProjectCreateRequest;
import com.lumenml.api.dto.ProjectDto;
import com.lumenml.api.dto.ProjectPatchRequest;
import com.lumenml.security.SecurityUtils;
import com.lumenml.service.ProjectService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(@Valid @RequestBody ProjectCreateRequest request) {
        return projectService.create(SecurityUtils.requireCurrentUser(), request);
    }

    @GetMapping
    public Page<ProjectDto> list(Pageable pageable) {
        return projectService.list(SecurityUtils.requireCurrentUser(), pageable);
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable UUID id) {
        return projectService.get(SecurityUtils.requireCurrentUser(), id);
    }

    @PatchMapping("/{id}")
    public ProjectDto patch(@PathVariable UUID id, @Valid @RequestBody ProjectPatchRequest request) {
        return projectService.patch(SecurityUtils.requireCurrentUser(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        projectService.delete(SecurityUtils.requireCurrentUser(), id);
    }
}
