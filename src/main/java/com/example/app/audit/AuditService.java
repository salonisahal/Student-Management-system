package com.example.app.audit;

import com.example.app.entity.AuditLog;
import com.example.app.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(Long userId, String action, String entityType, String entityId, String description, String ipAddress) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(userId);
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setDescription(description);
            entry.setIpAddress(ipAddress);
            entry.setTimestamp(LocalDateTime.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Auditing must never break the primary business flow.
            log.error("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }
}
