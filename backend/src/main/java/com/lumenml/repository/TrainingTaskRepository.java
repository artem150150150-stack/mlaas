package com.lumenml.repository;

import com.lumenml.domain.TaskStatus;
import com.lumenml.domain.TrainingTask;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingTaskRepository
        extends JpaRepository<TrainingTask, UUID>, JpaSpecificationExecutor<TrainingTask> {

    Page<TrainingTask> findByProjectId(UUID projectId, Pageable pageable);

    Page<TrainingTask> findByProjectIdAndStatus(UUID projectId, TaskStatus status, Pageable pageable);

    long countByStatus(TaskStatus status);

    @EntityGraph(attributePaths = {"project", "dataset", "mlModel"})
    @Query("select t from TrainingTask t where t.id = :id")
    Optional<TrainingTask> findDetailedById(@Param("id") UUID id);
}
