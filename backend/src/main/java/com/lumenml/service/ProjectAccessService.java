package com.lumenml.service;

import com.lumenml.domain.Project;
import com.lumenml.domain.UserRole;
import com.lumenml.exception.ForbiddenException;
import com.lumenml.exception.NotFoundException;
import com.lumenml.repository.ProjectRepository;
import com.lumenml.security.AuthPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;

    public Project requireForUser(UUID projectId, AuthPrincipal user) {
        Project p = projectRepository
                .findDetailedById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        if (user.role() != UserRole.ADMIN && !p.getOwner().getId().equals(user.id())) {
            throw new ForbiddenException("No access to project");
        }
        return p;
    }
}
