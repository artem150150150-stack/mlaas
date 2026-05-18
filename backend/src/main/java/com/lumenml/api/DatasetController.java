package com.lumenml.api;

import com.lumenml.api.dto.DatasetDto;
import com.lumenml.api.dto.DatasetUploadMetadata;
import com.lumenml.security.SecurityUtils;
import com.lumenml.service.DatasetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DatasetDto upload(
            @PathVariable UUID projectId,
            @Valid @RequestPart("metadata") DatasetUploadMetadata metadata,
            @RequestPart("file") MultipartFile file)
            throws Exception {
        return datasetService.upload(SecurityUtils.requireCurrentUser(), projectId, metadata, file);
    }

    @GetMapping
    public List<DatasetDto> list(@PathVariable UUID projectId) {
        return datasetService.list(SecurityUtils.requireCurrentUser(), projectId);
    }
}
