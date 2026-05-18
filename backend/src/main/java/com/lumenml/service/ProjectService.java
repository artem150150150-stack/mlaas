package com.lumenml.service;

import com.lumenml.api.dto.ProjectCreateRequest;
import com.lumenml.api.dto.ProjectDto;
import com.lumenml.api.dto.ProjectPatchRequest;
import com.lumenml.api.mapper.ApiMapper;
import com.lumenml.domain.Project;
import com.lumenml.domain.User;
import com.lumenml.repository.ProjectRepository;
import com.lumenml.repository.UserRepository;
import com.lumenml.security.AuthPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;
    private final ApiMapper apiMapper;

    @Transactional
    public ProjectDto create(AuthPrincipal principal, ProjectCreateRequest request) {
        User owner = userRepository.getReferenceById(principal.id());
        Project p = Project.builder()
                .owner(owner)
                .name(request.name().trim())
                .description(request.description())
                .build();
        return apiMapper.toProjectDto(projectRepository.save(p));
    }

    @Transactional(readOnly = true)
    public Page<ProjectDto> list(AuthPrincipal principal, Pageable pageable) {
        return projectRepository.findByOwnerId(principal.id(), pageable).map(apiMapper::toProjectDto);
    }

    @Transactional(readOnly = true)
    public ProjectDto get(AuthPrincipal principal, UUID id) {
        Project p = projectAccessService.requireForUser(id, principal);
        return apiMapper.toProjectDto(p);
    }

    @Transactional
    public ProjectDto patch(AuthPrincipal principal, UUID id, ProjectPatchRequest request) {
        Project p = projectAccessService.requireForUser(id, principal);
        if (request.name() != null) {
            p.setName(request.name().trim());
        }
        if (request.description() != null) {
            p.setDescription(request.description());
        }
        return apiMapper.toProjectDto(projectRepository.save(p));
    }

    @Transactional
    public void delete(AuthPrincipal principal, UUID id) {
        Project p = projectAccessService.requireForUser(id, principal);
        projectRepository.delete(p);
    }
}
