package com.lumenml.repository;

import com.lumenml.domain.TrainingMetrics;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingMetricsRepository extends JpaRepository<TrainingMetrics, UUID> {

    Optional<TrainingMetrics> findByTrainingTaskId(UUID trainingTaskId);
}
