package com.lumenml.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumenml.domain.AuditLog;
import com.lumenml.domain.User;
import com.lumenml.repository.AuditLogRepository;
import com.lumenml.repository.UserRepository;
import com.lumenml.security.AuthPrincipal;
import com.lumenml.security.SecurityUtils;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional
    public void log(String action, String entityType, UUID entityId, String ip, Map<String, Object> metadata) {
        try {
            AuthPrincipal principal = null;
            try {
                principal = SecurityUtils.requireCurrentUser();
            } catch (Exception ignored) {
                // not authenticated
            }
            User user = principal != null ? userRepository.getReferenceById(principal.id()) : null;
            String metaJson = metadata == null ? null : objectMapper.writeValueAsString(metadata);
            AuditLog log = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .ipAddress(ip)
                    .metadataJson(metaJson)
                    .build();
            auditLogRepository.save(log);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
