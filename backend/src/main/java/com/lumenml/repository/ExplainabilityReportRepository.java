package com.lumenml.repository;

import com.lumenml.domain.ExplainabilityReport;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExplainabilityReportRepository extends JpaRepository<ExplainabilityReport, UUID> {

    Optional<ExplainabilityReport> findByTrainingTaskId(UUID trainingTaskId);
}
