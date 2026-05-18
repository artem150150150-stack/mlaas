package com.lumenml.repository;

import com.lumenml.domain.MlModel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MlModelRepository extends JpaRepository<MlModel, UUID> {

    List<MlModel> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
