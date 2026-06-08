package dev.thilanka.resolvr.service;

import dev.thilanka.resolvr.config.ResolvrProperties;
import dev.thilanka.resolvr.model.entity.Attachment;
import dev.thilanka.resolvr.repository.AttachmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Two retention policies, run nightly at 02:00 in this order:
 *
 *  1. Quota policy  — if total stored bytes > 25 GB, delete oldest files
 *                     until usage drops to ≤ 20 GB.
 *
 *  2. Age policy    — delete any file older than max-age-days (default 1095 = 3 years).
 *
 * Quota runs first because it enforces the hard limit. Age runs second as
 * ongoing data hygiene. Both log every deletion for audit.
 *
 * Files are deleted from MinIO via AttachmentService.deleteInternal(),
 * which also updates the storage_stats counter in Postgres.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageCleanupService {

    private final AttachmentService    attachmentService;
    private final AttachmentRepository attachmentRepo;
    private final ResolvrProperties props;

    /** Nightly at 02:00 server time. */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runNightlyCleanup() {
        long before = attachmentService.currentUsageBytes();
        log.info("=== Storage cleanup started — usage: {} MB ===", mb(before));

        runQuotaCleanup();
        runAgeCleanup();

        long after = attachmentService.currentUsageBytes();
        log.info("=== Storage cleanup complete — usage: {} MB (freed {} MB) ===",
                mb(after), mb(before - after));
    }

    // ── Quota policy ────────────────────────────────────────────

    private void runQuotaCleanup() {
        long current = attachmentService.currentUsageBytes();
        if (current <= props.storage().maxBytes()) {
            log.info("[QUOTA] OK — {} MB / {} MB", mb(current), mb(props.storage().maxBytes()));
            return;
        }

        log.warn("[QUOTA] Over limit ({} MB > {} MB). Deleting oldest files to reach {} MB.",
                mb(current), mb(props.storage().maxBytes()), mb(props.storage().targetBytes()));

        int deleted = 0;
        while (attachmentService.currentUsageBytes() > props.storage().targetBytes()) {
            List<Attachment> batch = attachmentRepo.findTop50ByOrderByCreatedAtAsc();
            if (batch.isEmpty()) break;

            for (Attachment att : batch) {
                if (attachmentService.currentUsageBytes() <= props.storage().targetBytes()) break;
                log.info("[QUOTA] Deleting id={} entry={}:{} name='{}' size={} KB uploaded={}",
                        att.getId(), att.getEntryType(), att.getEntryId(),
                        att.getOriginalName(), att.getFileSizeBytes() / 1024, att.getCreatedAt());
                attachmentService.deleteInternal(att);
                deleted++;
            }
        }
        log.info("[QUOTA] {} files deleted. Usage now: {} MB", deleted,
                mb(attachmentService.currentUsageBytes()));
    }

    // ── Age policy ──────────────────────────────────────────────

    private void runAgeCleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(props.storage().maxAgeDays());
        List<Attachment> aged = attachmentRepo.findByCreatedAtBefore(cutoff);

        if (aged.isEmpty()) {
            log.info("[AGE] No files older than {} days.", props.storage().maxAgeDays());
            return;
        }

        log.info("[AGE] {} files older than {} days — deleting.", aged.size(), props.storage().maxAgeDays());
        for (Attachment att : aged) {
            log.info("[AGE] Deleting id={} entry={}:{} name='{}' uploaded={}",
                    att.getId(), att.getEntryType(), att.getEntryId(),
                    att.getOriginalName(), att.getCreatedAt());
            attachmentService.deleteInternal(att);
        }
        log.info("[AGE] {} files deleted.", aged.size());
    }

    private static long mb(long bytes) { return bytes / 1024 / 1024; }
}
