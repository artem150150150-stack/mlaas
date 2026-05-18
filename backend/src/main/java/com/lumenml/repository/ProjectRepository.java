package com.lumenml.repository;

import com.lumenml.domain.Project;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findByOwnerId(UUID ownerId, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    @Query("select p from Project p where p.id = :id")
    Optional<Project> findDetailedById(@Param("id") UUID id);
}
