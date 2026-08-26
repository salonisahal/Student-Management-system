package com.example.app.mapper;

import com.example.app.dto.AuditLogDto;
import com.example.app.entity.AuditLog;

public final class AuditLogMapper {
    private AuditLogMapper() {
    }

    public static AuditLogDto toDto(AuditLog a) {
        if (a == null) return null;
        AuditLogDto dto = new AuditLogDto();
        dto.setId(a.getId());
        dto.setUserId(a.getUserId());
        dto.setAction(a.getAction());
        dto.setEntityType(a.getEntityType());
        dto.setEntityId(a.getEntityId());
        dto.setDescription(a.getDescription());
        dto.setIpAddress(a.getIpAddress());
        dto.setTimestamp(a.getTimestamp());
        return dto;
    }
}
