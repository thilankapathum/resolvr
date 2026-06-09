package dev.thilanka.resolvr.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "ANALYSIS" or "SOLUTION" */
    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    /** Original client filename — stored for display and download headers. */
    @Column(name = "original_name", nullable = false)
    private String originalName;

    /**
     * MinIO object key — UUID-based path within the bucket.
     * Format: {entryType}/{entryId}/{uuid}.{ext}
     * e.g.  ANALYSIS/42/f3a1b9c2-....jpg
     */
    @Column(name = "object_key", nullable = false, unique = true, length = 500)
    private String objectKey;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
