package com.example.app.service;

import com.example.app.dto.AuditLogDto;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.AuditLogMapper;
import com.example.app.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(Long userId, String action, String entityType, LocalDateTime dateFrom,
                                            LocalDateTime dateTo, Pageable pageable) {
        return auditLogRepository.search(userId, action, entityType, dateFrom, dateTo, pageable).map(AuditLogMapper::toDto);
    }

    @Transactional(readOnly = true)
    public AuditLogDto getAuditLog(Long id) {
        return auditLogRepository.findById(id)
                .map(AuditLogMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + id));
    }
}
