package dev.thilanka.resolvr.dto.response;

import dev.thilanka.resolvr.enums.AuditAction;
import dev.thilanka.resolvr.enums.ComplaintStatus;
import dev.thilanka.resolvr.model.entity.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id, AuditAction action,
        ComplaintStatus fromStatus, ComplaintStatus toStatus,
        String notes, String actorName, String actorRole,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(), log.getAction(),
                log.getFromStatus(), log.getToStatus(),
                log.getNotes(),
                log.getActor().getFullName(),
                log.getActor().getRole() != null ? log.getActor().getRole().name() : "UNKNOWN",
                log.getCreatedAt()
        );
    }
}
