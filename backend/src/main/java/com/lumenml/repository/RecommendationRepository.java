package com.lumenml.repository;

import com.lumenml.domain.Recommendation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByTrainingTaskIdOrderByCreatedAtAsc(UUID trainingTaskId);
}
