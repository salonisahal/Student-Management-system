package com.example.app.repository;

import com.example.app.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:dateFrom IS NULL OR a.timestamp >= :dateFrom) AND " +
            "(:dateTo IS NULL OR a.timestamp <= :dateTo)")
    Page<AuditLog> search(@Param("userId") Long userId, @Param("action") String action,
                           @Param("entityType") String entityType, @Param("dateFrom") LocalDateTime dateFrom,
                           @Param("dateTo") LocalDateTime dateTo, Pageable pageable);
}
