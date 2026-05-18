package com.lumenml.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "explainability_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExplainabilityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_task_id", nullable = false, unique = true)
    private TrainingTask trainingTask;

    @Column(name = "feature_importance_json", nullable = false, columnDefinition = "TEXT")
    private String featureImportanceJson;

    @Column(name = "shap_values_json", columnDefinition = "TEXT")
    private String shapValuesJson;

    @Column(name = "shap_plot_uri")
    private String shapPlotUri;

    @Column(name = "lime_explanations_json", columnDefinition = "TEXT")
    private String limeExplanationsJson;

    @Column(name = "fairness_metrics_json", columnDefinition = "TEXT")
    private String fairnessMetricsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
