package dev.thilanka.resolvr.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "storage_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageStat {

    @Id
    private Integer id;

    @Column(name = "total_bytes", nullable = false)
    private long totalBytes;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
