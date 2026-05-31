package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.model.entity.AnalysisEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisEntryRepository extends JpaRepository<AnalysisEntry, Long> {
    List<AnalysisEntry> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
    Optional<AnalysisEntry> findTopByComplaintIdAndAuthorIdOrderByCreatedAtDesc(Long complaintId, Long authorId);
}
