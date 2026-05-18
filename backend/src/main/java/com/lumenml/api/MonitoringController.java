package com.lumenml.api;

import com.lumenml.api.dto.DriftSimulateRequest;
import com.lumenml.api.dto.MonitoringDtos.MonitoringDashboard;
import com.lumenml.service.MonitoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/dashboard")
    public MonitoringDashboard dashboard() {
        return monitoringService.dashboard();
    }

    @PostMapping("/drift/simulate")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void simulateDrift(@Valid @RequestBody DriftSimulateRequest request) {
        monitoringService.simulateDrift(request);
    }
}
