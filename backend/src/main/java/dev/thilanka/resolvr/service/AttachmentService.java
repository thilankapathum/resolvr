package dev.thilanka.resolvr.service;

import dev.thilanka.resolvr.config.ResolvrProperties;
import dev.thilanka.resolvr.dto.response.AttachmentResponse;
import dev.thilanka.resolvr.exception.BadRequestException;
import dev.thilanka.resolvr.exception.ForbiddenException;
import dev.thilanka.resolvr.exception.ResourceNotFoundException;
import dev.thilanka.resolvr.model.entity.Attachment;
import dev.thilanka.resolvr.model.entity.Complaint;
import dev.thilanka.resolvr.model.entity.StorageStat;
import dev.thilanka.resolvr.model.entity.User;
import dev.thilanka.resolvr.repository.AttachmentRepository;
import dev.thilanka.resolvr.repository.ComplaintRepository;
import dev.thilanka.resolvr.repository.StorageStatRepository;
import dev.thilanka.resolvr.repository.UserRepository;
import dev.thilanka.resolvr.security.UserDetailsImpl;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import io.minio.*;
import net.coobird.thumbnailator.Thumbnails;


import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    // ── Allowed MIME types ──────────────────────────────────────
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo"
    );
    private static final Set<String> ALLOWED_DOC_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );
    private static final Set<String> ALL_ALLOWED_TYPES;
    static {
        var all = new HashSet<String>();
        all.addAll(ALLOWED_IMAGE_TYPES);
        all.addAll(ALLOWED_VIDEO_TYPES);
        all.addAll(ALLOWED_DOC_TYPES);
        ALL_ALLOWED_TYPES = Collections.unmodifiableSet(all);
    }

    private final MinioClient minioClient;
    private final AttachmentRepository attachmentRepo;
    private final StorageStatRepository storageStatRepo;
    private final ComplaintRepository complaintRepo;
    private final UserRepository userRepo;
    private final ResolvrProperties props;

    // ── Upload ─────────────────────────────────────────────────

    @Transactional
    public AttachmentResponse upload(String entryType,
                                     Long entryId,
                                     Long complaintId,
                                     MultipartFile file,
                                     UserDetailsImpl uploader) {

        validateMimeType(file);
        validateFileSize(file);
        validateQuota(file.getSize());

        Complaint complaint = complaintRepo.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        User user = userRepo.findById(uploader.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String originalName = sanitiseFilename(file.getOriginalFilename());
        boolean isImage     = ALLOWED_IMAGE_TYPES.contains(file.getContentType());

        // Images are always stored as JPEG after compression
        String storedExt  = isImage ? ".jpg" : extension(originalName);
        String objectKey  = entryType.toUpperCase() + "/" + entryId + "/" + UUID.randomUUID() + storedExt;
        String mimeType   = isImage ? "image/jpeg" : file.getContentType();

        long storedBytes;
        try {
            storedBytes = uploadToMinio(file, objectKey, mimeType, isImage);
        } catch (Exception e) {
            log.error("MinIO upload failed for key {}", objectKey, e);
            throw new BadRequestException("Failed to store file. Please try again.");
        }

        Attachment attachment = Attachment.builder()
                .entryType(entryType.toUpperCase())
                .entryId(entryId)
                .complaint(complaint)
                .uploadedBy(user)
                .originalName(originalName)
                .objectKey(objectKey)
                .mimeType(mimeType)
                .fileSizeBytes(storedBytes)
                .build();

        attachmentRepo.save(attachment);
        incrementStorage(storedBytes);

        log.info("Attachment uploaded: key={} entry={}:{} size={}KB by user={}",
                objectKey, entryType, entryId, storedBytes / 1024, uploader.getId());

        return AttachmentResponse.from(attachment, presign(objectKey));
    }

    // ── List ───────────────────────────────────────────────────

    public List<AttachmentResponse> listForEntry(String entryType, Long entryId) {
        return attachmentRepo.findByEntryTypeAndEntryId(entryType.toUpperCase(), entryId)
                .stream()
                .map(a -> AttachmentResponse.from(a, presign(a.getObjectKey())))
                .toList();
    }

    // ── Delete (user-initiated) ─────────────────────────────────

    @Transactional
    public void delete(Long attachmentId, UserDetailsImpl caller) {
        Attachment att = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        boolean isOwner = att.getUploadedBy().getId().equals(caller.getId());
        boolean isPrivileged = caller.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

        if (!isOwner && !isPrivileged) {
            throw new ForbiddenException("You are not allowed to delete this attachment.");
        }
        physicalDelete(att);
    }

    /** Internal delete — called by the cleanup scheduler (no auth check). */
    @Transactional
    public void deleteInternal(Attachment att) {
        physicalDelete(att);
    }

    // ── Storage counter helpers ─────────────────────────────────

    public long currentUsageBytes() {
        return storageStatRepo.findById(1).map(StorageStat::getTotalBytes).orElse(0L);
    }

    @Transactional
    public void incrementStorage(long bytes) {
        StorageStat stat = storageStatRepo.findForUpdate()
                .orElseThrow(() -> new IllegalStateException("storage_stats row missing"));
        stat.setTotalBytes(stat.getTotalBytes() + bytes);
        stat.setUpdatedAt(LocalDateTime.now());
        storageStatRepo.save(stat);
    }

    @Transactional
    public void decrementStorage(long bytes) {
        StorageStat stat = storageStatRepo.findForUpdate()
                .orElseThrow(() -> new IllegalStateException("storage_stats row missing"));
        stat.setTotalBytes(Math.max(0, stat.getTotalBytes() - bytes));
        stat.setUpdatedAt(LocalDateTime.now());
        storageStatRepo.save(stat);
    }

    // ── Private helpers ─────────────────────────────────────────

    private void physicalDelete(Attachment att) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(props.minio().bucket())
                            .object(att.getObjectKey())
                            .build());
        } catch (Exception e) {
            log.warn("MinIO delete failed for key {}: {}", att.getObjectKey(), e.getMessage());
        }
        decrementStorage(att.getFileSizeBytes());
        attachmentRepo.delete(att);
        log.info("Attachment deleted: key={} entry={}:{}",
                att.getObjectKey(), att.getEntryType(), att.getEntryId());
    }

    /**
     * Uploads the file to MinIO.
     * Images are compressed first (WhatsApp-level: max 1920px, 82% JPEG quality).
     * Returns actual bytes stored (post-compression for images).
     */
    private long uploadToMinio(MultipartFile file, String objectKey, String mimeType, boolean compress)
            throws Exception {

        if (compress) {
            // Compress into an in-memory buffer — no temp files on disk
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (InputStream in = file.getInputStream()) {
                Thumbnails.of(in)
                        .size(props.storage().imageMaxPx(), props.storage().imageMaxPx())
                        .keepAspectRatio(true)
                        .outputQuality(props.storage().imageQuality())
                        .outputFormat("jpg")
                        .toOutputStream(buf);
            }
            byte[] compressed = buf.toByteArray();
            try (InputStream stream = new ByteArrayInputStream(compressed)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(props.minio().bucket())
                                .object(objectKey)
                                .stream(stream, (long) compressed.length, (long) -1)
                                .contentType(mimeType)
                                .build());
            }
            return compressed.length;
        }

        // Non-image: stream directly to MinIO
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.minio().bucket())
                            .object(objectKey)
                            .stream(in, file.getSize(), (long) -1)
                            .contentType(mimeType)
                            .build());
        }
        return file.getSize();
    }

    /**
     * Generates a presigned GET URL valid for resolvr.minio.presign-expiry-minutes.
     * The client uses this URL directly — no backend proxying needed.
     */
    private String presign(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(props.minio().bucket())
                            .object(objectKey)
                            .expiry(props.minio().presignExpiryMinutes(), TimeUnit.MINUTES)
                            .build());
        } catch (Exception e) {
            log.error("Failed to presign object key {}", objectKey, e);
            return "";
        }
    }

    private void validateMimeType(MultipartFile file) {
        String mime = file.getContentType();
        if (mime == null || !ALL_ALLOWED_TYPES.contains(mime)) {
            throw new BadRequestException(
                    "File type not allowed. Supported: images (JPEG/PNG/WebP/GIF), " +
                            "videos (MP4/WebM/MOV/AVI), documents (PDF/DOC/DOCX/TXT).");
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > props.storage().maxFileBytes()) {
            long mb = props.storage().maxFileBytes() / (1024 * 1024);
            throw new BadRequestException("File exceeds the maximum allowed size of " + mb + " MB.");
        }
    }

    private void validateQuota(long incomingBytes) {
        long current = storageStatRepo.findById(1)
                .map(StorageStat::getTotalBytes).orElse(0L);
        if (current + incomingBytes > props.storage().maxBytes()) {
            throw new BadRequestException("Storage quota exceeded. Contact the system administrator.");
        }
    }

    private static String sanitiseFilename(String name) {
        if (name == null || name.isBlank()) return "upload";
        return new File(name).getName().replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot).toLowerCase() : "";
    }
}
