package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}
