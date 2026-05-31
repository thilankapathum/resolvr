package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.model.entity.ComplaintRefSequence;
import dev.thilanka.resolvr.model.entity.ComplaintRefSequenceId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ComplaintRefSequenceRepository extends JpaRepository<ComplaintRefSequence, ComplaintRefSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ComplaintRefSequence s WHERE s.districtId = :districtId AND s.yearMonth = :yearMonth")
    Optional<ComplaintRefSequence> findByDistrictIdAndYearMonthLocked(
            @Param("districtId") Long districtId,
            @Param("yearMonth") String yearMonth
    );
}
