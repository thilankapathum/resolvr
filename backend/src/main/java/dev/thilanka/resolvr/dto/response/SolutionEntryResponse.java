package dev.thilanka.resolvr.dto.response;

import dev.thilanka.resolvr.model.entity.SolutionEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolutionEntryResponse(
        Long id, String content, LocalDate solutionTargetDate, String remarks,
        boolean edited, Long authorId, String authorName, String authorRole,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static SolutionEntryResponse from(SolutionEntry e) {
        return new SolutionEntryResponse(
                e.getId(), e.getContent(), e.getSolutionTargetDate(), e.getRemarks(),
                e.isEdited(),
                e.getAuthor().getId(), e.getAuthor().getFullName(),
                e.getAuthor().getRole() != null ? e.getAuthor().getRole().name() : "UNKNOWN",
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
