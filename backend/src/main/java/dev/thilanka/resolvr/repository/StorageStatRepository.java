package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.model.entity.StorageStat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StorageStatRepository extends JpaRepository<StorageStat, Integer> {

    /**
     * Pessimistic write lock prevents concurrent uploads from racing
     * and corrupting the running byte counter.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StorageStat s WHERE s.id = 1")
    Optional<StorageStat> findForUpdate();
}
