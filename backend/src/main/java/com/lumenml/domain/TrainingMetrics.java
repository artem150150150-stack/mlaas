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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "training_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_task_id", nullable = false, unique = true)
    private TrainingTask trainingTask;

    private Double accuracy;

    @Column(name = "precision_macro")
    private Double precisionMacro;

    @Column(name = "recall_macro")
    private Double recallMacro;

    @Column(name = "f1_macro")
    private Double f1Macro;

    private Double rmse;

    @Column(name = "confusion_matrix_json", columnDefinition = "TEXT")
    private String confusionMatrixJson;

    @Column(name = "train_score")
    private Double trainScore;

    @Column(name = "val_score")
    private Double valScore;

    @Column(name = "overfitting_estimate")
    private Double overfittingEstimate;
}
