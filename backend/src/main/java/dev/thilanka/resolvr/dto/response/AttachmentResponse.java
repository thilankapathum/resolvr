package dev.thilanka.resolvr.dto.response;

import dev.thilanka.resolvr.model.entity.Attachment;

import java.time.LocalDateTime;

public record AttachmentResponse(
        Long   id,
        String entryType,
        Long   entryId,
        String originalName,
        String mimeType,
        long   fileSizeBytes,
        LocalDateTime createdAt,
        String uploadedByName,
        /** Time-limited presigned URL — valid for resolvr.minio.presign-expiry-minutes. */
        String url
) {
    /** Called from AttachmentService which supplies the presigned URL. */
    public static AttachmentResponse from(Attachment a, String presignedUrl) {
        return new AttachmentResponse(
                a.getId(),
                a.getEntryType(),
                a.getEntryId(),
                a.getOriginalName(),
                a.getMimeType(),
                a.getFileSizeBytes(),
                a.getCreatedAt(),
                a.getUploadedBy().getFullName(),
                presignedUrl
        );
    }
}