package dev.thilanka.resolvr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from application.properties / environment variables.
 *
 * resolvr.minio.endpoint          = http://minio:9000
 * resolvr.minio.access-key        = (from .env)
 * resolvr.minio.secret-key        = (from .env)
 * resolvr.minio.bucket            = resolvr-attachments
 * resolvr.minio.presign-expiry-minutes = 60
 *
 * resolvr.storage.max-bytes       = 26843545600   (25 GB)
 * resolvr.storage.target-bytes    = 21474836480   (20 GB)
 * resolvr.storage.max-file-bytes  = 104857600     (100 MB per file)
 * resolvr.storage.max-age-days    = 1095          (3 years)
 * resolvr.storage.image-max-px    = 1920
 * resolvr.storage.image-quality   = 0.82
 */

@ConfigurationProperties(prefix = "resolvr")
public record ResolvrProperties(
        Minio   minio,
        Storage storage
) {
    public record Minio(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket,
            int    presignExpiryMinutes
    ) {}

    public record Storage(
            long  maxBytes,
            long  targetBytes,
            long  maxFileBytes,
            int   maxAgeDays,
            int   imageMaxPx,
            float imageQuality
    ) {}
}
