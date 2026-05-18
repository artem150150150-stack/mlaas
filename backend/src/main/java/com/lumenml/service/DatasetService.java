package com.lumenml.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenml.api.dto.DatasetDto;
import com.lumenml.api.dto.DatasetUploadMetadata;
import com.lumenml.api.mapper.ApiMapper;
import com.lumenml.config.LumenMlProperties;
import com.lumenml.domain.Dataset;
import com.lumenml.domain.Project;
import com.lumenml.exception.NotFoundException;
import com.lumenml.repository.DatasetRepository;
import com.lumenml.security.AuthPrincipal;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final ProjectAccessService projectAccessService;
    private final LumenMlProperties props;
    private final ObjectMapper objectMapper;
    private final ApiMapper apiMapper;

    @Transactional
    public DatasetDto upload(AuthPrincipal user, UUID projectId, DatasetUploadMetadata meta, MultipartFile file)
            throws Exception {
        Project project = projectAccessService.requireForUser(projectId, user);
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        Path dir = Path.of(props.getStorage().getDatasetsDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        UUID id = UUID.randomUUID();
        Path target = dir.resolve(id + ".csv");
        Files.copy(file.getInputStream(), target);

        CSVFormat fmt = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        long rows = 0;
        Map<String, Long> missing = new HashMap<>();
        Map<String, Double> min = new HashMap<>();
        Map<String, Double> max = new HashMap<>();
        for (String c : meta.featureColumns()) {
            missing.put(c, 0L);
            min.put(c, Double.POSITIVE_INFINITY);
            max.put(c, Double.NEGATIVE_INFINITY);
        }

        try (Reader reader = Files.newBufferedReader(target);
                CSVParser parser = CSVParser.parse(reader, fmt)) {
            LinkedHashSet<String> hdr = new LinkedHashSet<>(parser.getHeaderNames());
            if (!hdr.contains(meta.targetColumn())) {
                throw new IllegalArgumentException("Target column not found in CSV header");
            }
            for (String f : meta.featureColumns()) {
                if (!hdr.contains(f)) {
                    throw new IllegalArgumentException("Feature column not found: " + f);
                }
            }
            for (CSVRecord rec : parser) {
                rows++;
                for (String f : meta.featureColumns()) {
                    String raw = rec.isMapped(f) ? rec.get(f) : null;
                    if (raw == null || raw.isBlank()) {
                        missing.merge(f, 1L, Long::sum);
                        continue;
                    }
                    double v = Double.parseDouble(raw.trim());
                    min.put(f, Math.min(min.get(f), v));
                    max.put(f, Math.max(max.get(f), v));
                }
            }
        }

        Map<String, Map<String, Object>> stats = new HashMap<>();
        for (String f : meta.featureColumns()) {
            Map<String, Object> m = new HashMap<>();
            m.put("missingRatio", rows == 0 ? 0 : missing.getOrDefault(f, 0L) / (double) rows);
            m.put("min", min.get(f).isInfinite() ? null : min.get(f));
            m.put("max", max.get(f).isInfinite() ? null : max.get(f));
            stats.put(f, m);
        }

        Dataset ds = Dataset.builder()
                .project(project)
                .originalFilename(file.getOriginalFilename())
                .storageUri(target.toString())
                .taskType(meta.taskType())
                .targetColumn(meta.targetColumn())
                .featureColumnsJson(objectMapper.writeValueAsString(meta.featureColumns()))
                .rowCount(rows)
                .columnStatsJson(objectMapper.writeValueAsString(stats))
                .build();
        return apiMapper.toDatasetDto(datasetRepository.save(ds));
    }

    @Transactional(readOnly = true)
    public List<DatasetDto> list(AuthPrincipal user, UUID projectId) {
        projectAccessService.requireForUser(projectId, user);
        return datasetRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(apiMapper::toDatasetDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Dataset requireInProject(UUID projectId, UUID datasetId) {
        return datasetRepository
                .findByIdAndProject_Id(datasetId, projectId)
                .orElseThrow(() -> new NotFoundException("Dataset not found"));
    }
}
