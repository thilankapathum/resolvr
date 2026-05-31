package dev.thilanka.resolvr.dto.response;

import dev.thilanka.resolvr.model.entity.AnalysisEntry;

import java.time.LocalDateTime;

public record AnalysisEntryResponse(
        Long id, String content, boolean edited,
        Long authorId, String authorName, String authorRole,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static AnalysisEntryResponse from(AnalysisEntry e) {
        return new AnalysisEntryResponse(
                e.getId(), e.getContent(), e.isEdited(),
                e.getAuthor().getId(), e.getAuthor().getFullName(),
                e.getAuthor().getRole() != null ? e.getAuthor().getRole().name() : "UNKNOWN",
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
