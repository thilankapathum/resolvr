-- ── ATTACHMENTS ──────────────────────────────────────────────────────────────
-- Tracks files stored in MinIO. No file bytes are stored here — only metadata.
-- entry_type: 'ANALYSIS' | 'SOLUTION'
-- object_key: the MinIO object key (UUID-based, bucket-relative path)

CREATE TABLE attachments (
    id              BIGSERIAL    PRIMARY KEY,
    entry_type      VARCHAR(10)  NOT NULL CHECK (entry_type IN ('ANALYSIS', 'SOLUTION')),
    entry_id        BIGINT       NOT NULL,
    complaint_id    BIGINT       NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    uploaded_by_id  BIGINT       NOT NULL REFERENCES users(id),
    original_name   VARCHAR(255) NOT NULL,
    object_key      VARCHAR(500) NOT NULL UNIQUE,
    mime_type       VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT       NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_entry     ON attachments(entry_type, entry_id);
CREATE INDEX idx_attachments_complaint ON attachments(complaint_id);
CREATE INDEX idx_attachments_created   ON attachments(created_at);

-- ── STORAGE STATS ─────────────────────────────────────────────────────────────
-- Single-row counter updated atomically on every upload/delete.
-- Avoids expensive MinIO bucket stats calls on every upload.

CREATE TABLE storage_stats (
                               id          INT     PRIMARY KEY DEFAULT 1 CHECK (id = 1),
                               total_bytes BIGINT  NOT NULL DEFAULT 0,
                               updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO storage_stats(id, total_bytes) VALUES (1, 0);