package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.model.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByEntryTypeAndEntryId(String entryType, Long entryId);

    /** Age-based cleanup: all files older than the given cutoff. */
    List<Attachment> findByCreatedAtBefore(LocalDateTime cutoff);

    /**
     * Quota-based cleanup: oldest files first, bounded by limit.
     * Fetched in batches so the cleanup loop never loads the full table.
     */
    List<Attachment> findTop50ByOrderByCreatedAtAsc();
}
