package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.model.entity.SolutionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolutionEntryRepository extends JpaRepository<SolutionEntry, Long> {
    List<SolutionEntry> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
    Optional<SolutionEntry> findTopByComplaintIdAndAuthorIdOrderByCreatedAtDesc(Long complaintId, Long authorId);
    boolean existsByComplaintIdAndAuthorId(Long complaintId, Long authorId);
}

