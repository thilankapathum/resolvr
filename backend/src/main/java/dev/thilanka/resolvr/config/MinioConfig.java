package dev.thilanka.resolvr.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MinioConfig {

    private final ResolvrProperties props;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(props.minio().endpoint())
                .credentials(props.minio().accessKey(), props.minio().secretKey())
                .build();
    }

    /**
     * Ensures the private bucket exists after the full application context is ready.
     * Using @EventListener(ContextRefreshedEvent) instead of @PostConstruct avoids
     * the circular reference that occurs when a @Configuration class tries to call
     * its own @Bean method during initialisation.
     *
     * The bucket is left with no public policy (MinIO default = private).
     * All object access goes exclusively through presigned URLs minted by AttachmentService.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void ensureBucket(ContextRefreshedEvent event) {
        // Guard against double-firing in parent/child application contexts
        if (event.getApplicationContext().getParent() != null) return;

        MinioClient client = event.getApplicationContext().getBean(MinioClient.class);
        String bucket = props.minio().bucket();
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket '{}' created.", bucket);
            } else {
                log.info("MinIO bucket '{}' already exists.", bucket);
            }
            log.info("MinIO ready — bucket '{}' is private. Files served via presigned URLs only.", bucket);
        } catch (Exception e) {
            throw new IllegalStateException("MinIO bucket initialisation failed: " + e.getMessage(), e);
        }
    }
}