package com.lumenml.repository;

import com.lumenml.domain.Dataset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {

    List<Dataset> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<Dataset> findByIdAndProject_Id(UUID id, UUID projectId);
}
