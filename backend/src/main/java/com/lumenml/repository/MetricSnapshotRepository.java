package com.lumenml.repository;

import com.lumenml.domain.MetricSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, UUID> {

    List<MetricSnapshot> findByTrainingTaskIdOrderByCapturedAtAsc(UUID trainingTaskId);
}
