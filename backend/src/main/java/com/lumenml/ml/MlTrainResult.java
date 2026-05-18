package com.lumenml.ml;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MlTrainResult {
    Double accuracy;
    Double precisionMacro;
    Double recallMacro;
    Double f1Macro;
    Double rmse;
    List<List<Integer>> confusionMatrix;
    Map<String, Double> featureImportance;
    double[][] shapSummary;
    List<Map<String, Object>> limeSamples;
    Double trainScore;
    Double valScore;
    Double overfittingEstimate;
    String[] featureNames;
}
