package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.enums.ComplaintStatus;
import dev.thilanka.resolvr.model.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>,
        JpaSpecificationExecutor<Complaint> {

    Optional<Complaint> findByRefNumber(String refNumber);

    // Assigned to a specific user
    Page<Complaint> findByAssignedToId(Long userId, Pageable pageable);

    // Created by a specific user
    Page<Complaint> findByCreatedById(Long userId, Pageable pageable);

    // By district
    Page<Complaint> findByDistrictId(Long districtId, Pageable pageable);

    // By status
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    // By district + status
    Page<Complaint> findByDistrictIdAndStatus(Long districtId, ComplaintStatus status, Pageable pageable);

    // All complaints in a region (for Manager/Head)
    @Query("""
        SELECT c FROM Complaint c
        WHERE c.district.region.id = :regionId
        ORDER BY c.createdAt DESC
        """)
    Page<Complaint> findByRegionId(@Param("regionId") Long regionId, Pageable pageable);

    // Complaints in a region with status filter
    @Query("""
        SELECT c FROM Complaint c
        WHERE c.district.region.id = :regionId AND c.status = :status
        """)
    Page<Complaint> findByRegionIdAndStatus(
            @Param("regionId") Long regionId,
            @Param("status") ComplaintStatus status,
            Pageable pageable
    );

    // For TO/Engineer: assigned to them OR created by them in their district
    @Query("""
        SELECT c FROM Complaint c
        WHERE c.assignedTo.id = :userId
           OR (c.createdBy.id = :userId AND c.assignedTo IS NULL)
        ORDER BY c.createdAt DESC
        """)
    Page<Complaint> findRelevantForUser(@Param("userId") Long userId, Pageable pageable);

    // Count by status for dashboard
    @Query("SELECT c.status, COUNT(c) FROM Complaint c WHERE c.district.region.id = :regionId GROUP BY c.status")
    List<Object[]> countByStatusForRegion(@Param("regionId") Long regionId);

    @Query("SELECT c.status, COUNT(c) FROM Complaint c WHERE c.assignedTo.id = :userId GROUP BY c.status")
    List<Object[]> countByStatusForUser(@Param("userId") Long userId);
}
