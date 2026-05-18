package com.lumenml.ml;

import com.lumenml.domain.ModelKind;
import com.lumenml.domain.TaskType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Component;
import smile.classification.DataFrameClassifier;
import smile.classification.GradientTreeBoost;
import smile.classification.LogisticRegression;
import smile.classification.RandomForest;
import smile.classification.Classifier;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.vector.IntVector;
import smile.feature.importance.SHAP;
import smile.io.Read;
import smile.regression.LinearModel;
import smile.regression.OLS;
import smile.validation.ClassificationMetrics;
import smile.validation.metric.ConfusionMatrix;

@Slf4j
@Component
public class MlTrainingEngine {

    private static final Random RNG = new Random(42);

    public MlTrainResult train(
            Path csvPath,
            TaskType taskType,
            ModelKind modelKind,
            String targetColumn,
            List<String> featureColumns,
            Map<String, Object> hyperparameters)
            throws Exception {

        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        DataFrame raw = Read.csv(csvPath, format);
        String[] selected = concat(featureColumns, targetColumn);
        DataFrame df = raw.select(selected).omitNullRows();
        if (df.nrow() < 16) {
            throw new IllegalArgumentException("Not enough rows after dropping missing values (min 16)");
        }
        DataFrame work = df.factorize(df.names());
        int n = work.nrow();
        List<Integer> order = IntStream.range(0, n).boxed().collect(Collectors.toList());
        Collections.shuffle(order, RNG);
        int split = (int) (n * 0.8);
        int[] trainIdx = order.subList(0, split).stream().mapToInt(Integer::intValue).toArray();
        int[] testIdx = order.subList(split, n).stream().mapToInt(Integer::intValue).toArray();
        DataFrame train = work.select(trainIdx);
        DataFrame test = work.select(testIdx);

        Formula formula = Formula.lhs(targetColumn);
        Properties props = toSmileProps(modelKind, hyperparameters);

        if (taskType == TaskType.CLASSIFICATION) {
            return trainClassification(
                    formula, train, test, modelKind, targetColumn, featureColumns, props);
        }
        return trainRegression(formula, train, test, modelKind, targetColumn, featureColumns, props);
    }

    private MlTrainResult trainClassification(
            Formula formula,
            DataFrame train,
            DataFrame test,
            ModelKind modelKind,
            String targetColumn,
            List<String> featureColumns,
            Properties props)
            throws Exception {

        int[] truth = intLabels(test, targetColumn);
        int[] predicted;
        double[] importance;
        SHAP<Tuple> shapModel = null;

        switch (modelKind) {
            case RANDOM_FOREST -> {
                RandomForest model = RandomForest.fit(formula, train, props);
                predicted = model.predict(test);
                importance = normalizeImportance(model.importance(), featureColumns);
                shapModel = model;
            }
            case XGBOOST -> {
                GradientTreeBoost model = GradientTreeBoost.fit(formula, train, props);
                predicted = model.predict(test);
                importance = normalizeImportance(model.importance(), featureColumns);
                shapModel = model;
            }
            case LOGISTIC_REGRESSION -> {
                Classifier.Trainer<double[], LogisticRegression> trainer =
                        (double[][] x, int[] y, Properties p) -> uniqueCount(y) <= 2
                                ? LogisticRegression.binomial(x, y, p)
                                : LogisticRegression.multinomial(x, y, p);
                DataFrameClassifier model = DataFrameClassifier.of(formula, train, props, trainer);
                predicted = model.predict(test);
                importance = uniformImportance(featureColumns.size());
            }
            case LINEAR_REGRESSION -> throw new IllegalArgumentException("Linear regression is for regression tasks");
            default -> throw new IllegalArgumentException("Unsupported model");
        }

        ClassificationMetrics cm = ClassificationMetrics.of(0.0, 0.0, truth, predicted);
        int[][] matrix = ConfusionMatrix.of(truth, predicted).matrix;
        List<List<Integer>> cmJson = Arrays.stream(matrix)
                .map(row -> Arrays.stream(row).boxed().collect(Collectors.toList()))
                .collect(Collectors.toList());

        double trainAcc = trainScoreClassification(formula, train, modelKind, props, targetColumn);
        double valAcc = cm.accuracy;
        double overfit = Math.max(0, trainAcc - valAcc);

        Map<String, Double> fiMap = toMap(featureColumns, importance);
        double[][] shap =
                shapModel != null ? meanAbsShap(shapModel, test, featureColumns.size()) : permutationFallback(fiMap);

        return MlTrainResult.builder()
                .accuracy(cm.accuracy)
                .precisionMacro(cm.precision)
                .recallMacro(cm.sensitivity)
                .f1Macro(cm.f1)
                .rmse(null)
                .confusionMatrix(cmJson)
                .featureImportance(fiMap)
                .shapSummary(shap)
                .limeSamples(buildLimeSamples(test, truth, predicted, featureColumns))
                .trainScore(trainAcc)
                .valScore(valAcc)
                .overfittingEstimate(overfit)
                .featureNames(featureColumns.toArray(String[]::new))
                .build();
    }

    private MlTrainResult trainRegression(
            Formula formula,
            DataFrame train,
            DataFrame test,
            ModelKind modelKind,
            String targetColumn,
            List<String> featureColumns,
            Properties props)
            throws Exception {

        double[] truth = test.doubleVector(targetColumn).array();
        double[] predicted;
        double[] importance;
        SHAP<Tuple> shapModel = null;

        switch (modelKind) {
            case RANDOM_FOREST -> {
                smile.regression.RandomForest model = smile.regression.RandomForest.fit(formula, train, props);
                predicted = model.predict(test);
                importance = normalizeImportance(model.importance(), featureColumns);
                shapModel = model;
            }
            case XGBOOST -> {
                smile.regression.GradientTreeBoost model =
                        smile.regression.GradientTreeBoost.fit(formula, train, props);
                predicted = model.predict(test);
                importance = normalizeImportance(model.importance(), featureColumns);
                shapModel = model;
            }
            case LINEAR_REGRESSION -> {
                LinearModel model = OLS.fit(formula, train, props);
                predicted = model.predict(test);
                double[] coefs = model.coefficients();
                int p = featureColumns.size();
                double[] slice = Arrays.copyOf(coefs, Math.min(p, coefs.length));
                importance = normalizeImportance(slice, featureColumns);
            }
            case LOGISTIC_REGRESSION -> throw new IllegalArgumentException("Logistic regression is for classification");
            default -> throw new IllegalArgumentException("Unsupported model");
        }

        double mse = 0;
        for (int i = 0; i < truth.length; i++) {
            double d = truth[i] - predicted[i];
            mse += d * d;
        }
        mse /= truth.length;
        double rmse = Math.sqrt(mse);

        double trainRmse = trainRmse(formula, train, modelKind, props, targetColumn);
        double valRmse = rmse;
        double overfit = Math.max(0, trainRmse - valRmse);

        Map<String, Double> fiMap = toMap(featureColumns, importance);
        double[][] shap =
                shapModel != null ? meanAbsShap(shapModel, test, featureColumns.size()) : permutationFallback(fiMap);

        return MlTrainResult.builder()
                .accuracy(null)
                .precisionMacro(null)
                .recallMacro(null)
                .f1Macro(null)
                .rmse(rmse)
                .confusionMatrix(null)
                .featureImportance(fiMap)
                .shapSummary(shap)
                .limeSamples(buildLimeRegressionSamples(test, truth, predicted, featureColumns))
                .trainScore(trainRmse)
                .valScore(valRmse)
                .overfittingEstimate(overfit)
                .featureNames(featureColumns.toArray(String[]::new))
                .build();
    }

    private static double trainScoreClassification(
            Formula formula,
            DataFrame train,
            ModelKind modelKind,
            Properties props,
            String targetColumn) {
        int[] y = intLabels(train, targetColumn);
        return switch (modelKind) {
            case RANDOM_FOREST -> {
                RandomForest m = RandomForest.fit(formula, train, props);
                int[] p = m.predict(train);
                yield accuracy(y, p);
            }
            case XGBOOST -> {
                GradientTreeBoost m = GradientTreeBoost.fit(formula, train, props);
                int[] p = m.predict(train);
                yield accuracy(y, p);
            }
            case LOGISTIC_REGRESSION -> {
                Classifier.Trainer<double[], LogisticRegression> trainer =
                        (double[][] x, int[] yy, Properties p) -> uniqueCount(yy) <= 2
                                ? LogisticRegression.binomial(x, yy, p)
                                : LogisticRegression.multinomial(x, yy, p);
                DataFrameClassifier m = DataFrameClassifier.of(formula, train, props, trainer);
                int[] p = m.predict(train);
                yield accuracy(y, p);
            }
            default -> 0;
        };
    }

    private static double trainRmse(
            Formula formula, DataFrame train, ModelKind modelKind, Properties props, String targetColumn) {
        double[] y = train.doubleVector(targetColumn).array();
        return switch (modelKind) {
            case RANDOM_FOREST -> {
                var m = smile.regression.RandomForest.fit(formula, train, props);
                yield rmse(y, m.predict(train));
            }
            case XGBOOST -> {
                var m = smile.regression.GradientTreeBoost.fit(formula, train, props);
                yield rmse(y, m.predict(train));
            }
            case LINEAR_REGRESSION -> {
                LinearModel m = OLS.fit(formula, train, props);
                yield rmse(y, m.predict(train));
            }
            default -> 0;
        };
    }

    private static double rmse(double[] truth, double[] pred) {
        double mse = 0;
        for (int i = 0; i < truth.length; i++) {
            double d = truth[i] - pred[i];
            mse += d * d;
        }
        return Math.sqrt(mse / truth.length);
    }

    private static double accuracy(int[] truth, int[] pred) {
        int ok = 0;
        for (int i = 0; i < truth.length; i++) {
            if (truth[i] == pred[i]) {
                ok++;
            }
        }
        return truth.length == 0 ? 0 : (double) ok / truth.length;
    }

    private static int uniqueCount(int[] y) {
        return (int) Arrays.stream(y).distinct().count();
    }

    private static double[] uniformImportance(int p) {
        double[] imp = new double[p];
        Arrays.fill(imp, 1.0 / p);
        return imp;
    }

    private static double[] normalizeImportance(double[] raw, List<String> featureColumns) {
        int len = Math.min(raw.length, featureColumns.size());
        double[] v = Arrays.copyOf(raw, len);
        if (len < featureColumns.size()) {
            v = Arrays.copyOf(v, featureColumns.size());
        }
        double sum = Arrays.stream(v).map(Math::abs).sum();
        if (sum <= 1e-9) {
            Arrays.fill(v, 1.0 / v.length);
            return v;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = Math.abs(v[i]) / sum;
        }
        return v;
    }

    private static Map<String, Double> toMap(List<String> names, double[] values) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            map.put(names.get(i), i < values.length ? values[i] : 0.0);
        }
        return map;
    }

    private static double[][] meanAbsShap(SHAP<Tuple> model, DataFrame test, int featureCount) {
        int samples = Math.min(24, test.nrow());
        double[] accum = new double[featureCount];
        for (int i = 0; i < samples; i++) {
            double[] row = model.shap(test.get(i));
            for (int j = 0; j < featureCount && j < row.length; j++) {
                accum[j] += Math.abs(row[j]);
            }
        }
        for (int j = 0; j < featureCount; j++) {
            accum[j] /= samples;
        }
        return new double[][] {accum};
    }

    private static double[][] permutationFallback(Map<String, Double> fi) {
        double[] row = fi.values().stream().mapToDouble(Double::doubleValue).toArray();
        return new double[][] {row};
    }

    private static List<Map<String, Object>> buildLimeSamples(
            DataFrame test, int[] truth, int[] predicted, List<String> features) {
        List<Map<String, Object>> out = new ArrayList<>();
        int m = Math.min(5, test.nrow());
        for (int i = 0; i < m; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("instance", i);
            row.put("truth", truth[i]);
            row.put("prediction", predicted[i]);
            for (String f : features) {
                row.put(f, test.getDouble(i, f));
            }
            row.put("localWeight", Math.abs(predicted[i] - truth[i]) + 0.01);
            out.add(row);
        }
        return out;
    }

    private static List<Map<String, Object>> buildLimeRegressionSamples(
            DataFrame test, double[] truth, double[] predicted, List<String> features) {
        List<Map<String, Object>> out = new ArrayList<>();
        int m = Math.min(5, test.nrow());
        for (int i = 0; i < m; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("instance", i);
            row.put("truth", truth[i]);
            row.put("prediction", predicted[i]);
            for (String f : features) {
                row.put(f, test.getDouble(i, f));
            }
            row.put("localError", Math.abs(predicted[i] - truth[i]));
            out.add(row);
        }
        return out;
    }

    private static int[] intLabels(DataFrame df, String column) {
        var col = df.column(column);
        if (col instanceof IntVector iv) {
            return iv.array();
        }
        int[] y = new int[df.nrow()];
        for (int i = 0; i < y.length; i++) {
            y[i] = (int) Math.round(df.getDouble(i, column));
        }
        return y;
    }

    private static String[] concat(List<String> features, String target) {
        String[] arr = new String[features.size() + 1];
        for (int i = 0; i < features.size(); i++) {
            arr[i] = features.get(i);
        }
        arr[features.size()] = target;
        return arr;
    }

    private static Properties toSmileProps(ModelKind kind, Map<String, Object> hp) {
        Properties p = new Properties();
        int trees = intOr(hp, "n_estimators", 200);
        int maxDepth = intOr(hp, "max_depth", 16);
        if (kind == ModelKind.RANDOM_FOREST) {
            p.setProperty("smile.random_forest.trees", String.valueOf(trees));
            p.setProperty("smile.random_forest.max_depth", String.valueOf(maxDepth));
        }
        if (kind == ModelKind.XGBOOST) {
            p.setProperty("smile.gradient_boost.trees", String.valueOf(trees));
            p.setProperty("smile.gradient_boost.max_depth", String.valueOf(maxDepth));
            p.setProperty("smile.gradient_boost.shrinkage", String.valueOf(doubleOr(hp, "learning_rate", 0.1)));
        }
        if (kind == ModelKind.LOGISTIC_REGRESSION) {
            p.setProperty("smile.logistic.regression.max_iterations", String.valueOf(intOr(hp, "max_iterations", 200)));
            p.setProperty("smile.logistic.regression.lambda", String.valueOf(1.0 / Math.max(0.001, doubleOr(hp, "C", 1.0))));
        }
        if (kind == ModelKind.LINEAR_REGRESSION) {
            p.setProperty("smile.ols.standard_error", "false");
        }
        return p;
    }

    private static int intOr(Map<String, Object> hp, String key, int def) {
        Object v = hp.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        return def;
    }

    private static double doubleOr(Map<String, Object> hp, String key, double def) {
        Object v = hp.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return def;
    }
}
