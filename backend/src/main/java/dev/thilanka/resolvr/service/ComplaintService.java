package dev.thilanka.resolvr.service;

import dev.thilanka.resolvr.dto.request.*;
import dev.thilanka.resolvr.dto.response.ComplaintResponse;
import dev.thilanka.resolvr.enums.AuditAction;
import dev.thilanka.resolvr.enums.ComplaintStatus;
import dev.thilanka.resolvr.enums.UserRole;
import dev.thilanka.resolvr.exception.BadRequestException;
import dev.thilanka.resolvr.exception.ForbiddenException;
import dev.thilanka.resolvr.exception.ResourceNotFoundException;
import dev.thilanka.resolvr.model.entity.*;
import dev.thilanka.resolvr.repository.*;
import dev.thilanka.resolvr.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintRefSequenceRepository refSeqRepository;
    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final AuditLogRepository auditLogRepository;
    private final AnalysisEntryRepository analysisEntryRepository;
    private final SolutionEntryRepository solutionEntryRepository;
    private final AttachmentService attachmentService;
    private final EmailService emailService;

    // ── Create ───────────────────────────────────────────────────

    @Transactional
    public ComplaintResponse createComplaint(CreateComplaintRequest request, UserDetailsImpl actor) {
        User currentUser = getUser(actor.getId());
        District district = districtRepository.findById(request.districtId())
                .orElseThrow(() -> new ResourceNotFoundException("District", request.districtId()));

        // Validate actor can create for this district
//        validateUserCanActOnDistrict(currentUser, district);

        LocalDateTime now = LocalDateTime.now();
        String refNumber = generateRefNumber(district, now);
        LocalDateTime targetDate = now.plusHours(request.priority().getTargetHours());

        Complaint.ComplaintBuilder builder = Complaint.builder()
                .refNumber(refNumber)
                .district(district)
                .status(ComplaintStatus.NOT_ASSIGNED)
                .priority(request.priority())
                .targetDate(targetDate)
                .createdBy(currentUser)
                .raisedBy(request.raisedBy())
                .customerName(request.customerName())
                .contactNumber(request.contactNumber())
                .msisdns(request.msisdns())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .issueCategory(request.issueCategory())
                .issueDescription(request.issueDescription())
                .issueDuration(request.issueDuration())
                .lastExperienced(request.lastExperienced())
                .technology(request.technology())
                .additionalInfo(request.additionalInfo())
                .deviceType(request.deviceType())
                .signalBars(request.signalBars())
                .usingVpnApn(request.usingVpnApn());

        // Optional immediate assignment
        if (request.assignedToId() != null) {
            User assignee = getUser(request.assignedToId());
            validateAssignee(assignee, district);
            builder.assignedTo(assignee).status(ComplaintStatus.NOT_STARTED);
        }

        Complaint complaint = complaintRepository.save(builder.build());

        // Audit log
        AuditLog audit = AuditLog.builder()
                .complaint(complaint)
                .actor(currentUser)
                .action(AuditAction.CREATED)
                .toStatus(complaint.getStatus())
                .notes("Complaint created" + (request.assignedToId() != null
                        ? " and assigned to " + complaint.getAssignedTo().getFullName() : ""))
                .build();
        auditLogRepository.save(audit);

        if (request.assignedToId() != null) {
            auditLogRepository.save(AuditLog.builder()
                    .complaint(complaint)
                    .actor(currentUser)
                    .action(AuditAction.ASSIGNED)
                    .fromStatus(ComplaintStatus.NOT_ASSIGNED)
                    .toStatus(ComplaintStatus.NOT_STARTED)
                    .notes("Assigned to " + complaint.getAssignedTo().getFullName())
                    .build());

            // Notify the assignee
            User assignee = complaint.getAssignedTo();
            emailService.sendComplaintAssignedEmail(
                    assignee.getEmail(),
                    assignee.getFullName(),
                    complaint.getRefNumber(),
                    complaint.getCustomerName(),
                    complaint.getDistrict().getName(),
                    complaint.getIssueCategory().toString(),
                    currentUser.getFullName()
            );
        }

        return ComplaintResponse.from(reload(complaint.getId()));
    }

    // ── Assign ───────────────────────────────────────────────────

    @Transactional
    public ComplaintResponse assignComplaint(Long complaintId, AssignComplaintRequest request,
                                             UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        if (complaint.getStatus() != ComplaintStatus.NOT_ASSIGNED
                && complaint.getStatus() != ComplaintStatus.NOT_STARTED) {
            throw new BadRequestException("Complaint can only be assigned when NOT_ASSIGNED or NOT_STARTED.");
        }

        User assignee = getUser(request.assignedToId());
        validateAssignee(assignee, complaint.getDistrict());

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setAssignedTo(assignee);
        complaint.setStatus(ComplaintStatus.NOT_STARTED);
        complaintRepository.save(complaint);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.ASSIGNED)
                .fromStatus(oldStatus).toStatus(ComplaintStatus.NOT_STARTED)
                .notes("Assigned to " + assignee.getFullName())
                .build());

        emailService.sendComplaintAssignedEmail(
                assignee.getEmail(),
                assignee.getFullName(),
                complaint.getRefNumber(),
                complaint.getCustomerName(),
                complaint.getDistrict().getName(),
                complaint.getIssueCategory().toString(),
                currentUser.getFullName()
        );

        return ComplaintResponse.from(reload(complaintId));
    }

    // ── Start ────────────────────────────────────────────────────

    @Transactional
    public ComplaintResponse startComplaint(Long complaintId, UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        validateIsAssignee(complaint, currentUser);

        if (complaint.getStatus() != ComplaintStatus.NOT_STARTED) {
            throw new BadRequestException("Complaint must be in NOT_STARTED status to start.");
        }

        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaintRepository.save(complaint);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.STARTED)
                .fromStatus(ComplaintStatus.NOT_STARTED).toStatus(ComplaintStatus.IN_PROGRESS)
                .build());

//        return ComplaintResponse.from(reload(complaintId));
        return toDetailResponse(reload(complaintId));

    }

    // ── Add Analysis ─────────────────────────────────────────────

    @Transactional
    public ComplaintResponse addAnalysis(Long complaintId, AddAnalysisRequest request,
                                         UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        validateCanContribute(complaint, currentUser);
        validateStatusForAnalysis(complaint);

        // Update serving site / coverage fields if provided (these are complaint-level fields)
        if (request.servingSitesCells() != null) complaint.setServingSitesCells(request.servingSitesCells());
        if (request.coverageQuality() != null) complaint.setCoverageQuality(request.coverageQuality());
        complaintRepository.save(complaint);

        AnalysisEntry entry = AnalysisEntry.builder()
                .complaint(complaint)
                .author(currentUser)
                .content(request.content())
                .build();
        analysisEntryRepository.save(entry);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.ANALYSIS_ADDED)
                .build());

//        return ComplaintResponse.from(reload(complaintId));
        return toDetailResponse(reload(complaintId));

    }

    @Transactional
    public ComplaintResponse editAnalysis(Long complaintId, Long entryId, AddAnalysisRequest request,
                                          UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        AnalysisEntry entry = analysisEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis entry", entryId));

        if (!entry.getAuthor().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only edit your own analysis entries.");
        }

        // Cannot edit if solution has been added by this user
        if (solutionEntryRepository.existsByComplaintIdAndAuthorId(complaintId, currentUser.getId())) {
            throw new BadRequestException("Cannot edit analysis after solution has been added.");
        }

        // Must be the latest entry by this author
        AnalysisEntry latest = analysisEntryRepository
                .findTopByComplaintIdAndAuthorIdOrderByCreatedAtDesc(complaintId, currentUser.getId())
                .orElseThrow();
        if (!latest.getId().equals(entryId)) {
            throw new BadRequestException("You can only edit your most recent analysis entry.");
        }

        entry.setContent(request.content());
        entry.setEdited(true);
        if (request.servingSitesCells() != null) complaint.setServingSitesCells(request.servingSitesCells());
        if (request.coverageQuality() != null) complaint.setCoverageQuality(request.coverageQuality());
        analysisEntryRepository.save(entry);
        complaintRepository.save(complaint);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.ANALYSIS_EDITED)
                .build());

//        return ComplaintResponse.from(reload(complaintId));
        return toDetailResponse(reload(complaintId));

    }

    // ── Add Solution ─────────────────────────────────────────────

    @Transactional
    public ComplaintResponse addSolution(Long complaintId, AddSolutionRequest request,
                                         UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        validateCanContribute(complaint, currentUser);
        validateStatusForSolution(complaint);

        // Must have at least one analysis entry
        List<AnalysisEntry> analyses = analysisEntryRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);
        if (analyses.isEmpty()) {
            throw new BadRequestException("Analysis must be added before solution.");
        }

        SolutionEntry entry = SolutionEntry.builder()
                .complaint(complaint)
                .author(currentUser)
                .content(request.content())
                .solutionTargetDate(request.solutionTargetDate())
                .remarks(request.remarks())
                .build();
        solutionEntryRepository.save(entry);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.SOLUTION_ADDED)
                .build());

//        return ComplaintResponse.from(reload(complaintId));
        return toDetailResponse(reload(complaintId));

    }

    @Transactional
    public ComplaintResponse editSolution(Long complaintId, Long entryId, AddSolutionRequest request,
                                          UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        SolutionEntry entry = solutionEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Solution entry", entryId));

        if (!entry.getAuthor().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only edit your own solution entries.");
        }

        // Cannot edit if complaint is RESOLVED or CLOSED
        if (complaint.getStatus() == ComplaintStatus.RESOLVED
                || complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new BadRequestException("Cannot edit solution after complaint is resolved or closed.");
        }

        SolutionEntry latest = solutionEntryRepository
                .findTopByComplaintIdAndAuthorIdOrderByCreatedAtDesc(complaintId, currentUser.getId())
                .orElseThrow();
        if (!latest.getId().equals(entryId)) {
            throw new BadRequestException("You can only edit your most recent solution entry.");
        }

        entry.setContent(request.content());
        entry.setSolutionTargetDate(request.solutionTargetDate());
        entry.setRemarks(request.remarks());
        entry.setEdited(true);
        solutionEntryRepository.save(entry);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.SOLUTION_EDITED)
                .build());

//        return ComplaintResponse.from(reload(complaintId));
        return toDetailResponse(reload(complaintId));

    }

    // ── Escalate ─────────────────────────────────────────────────

    @Transactional
    public ComplaintResponse escalateToEngineer(Long complaintId, EscalateRequest request,
                                                UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        if (currentUser.getRole() != UserRole.TECHNICAL_OFFICER) {
            throw new ForbiddenException("Only Technical Officers can escalate to an Engineer.");
        }
        validateIsAssignee(complaint, currentUser);

        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new BadRequestException("Can only escalate an IN_PROGRESS complaint.");
        }

        // Must have analysis
        List<AnalysisEntry> analyses = analysisEntryRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId);
        if (analyses.isEmpty()) {
            throw new BadRequestException("Analysis must be added before escalating.");
        }

        User engineer = getUser(request.engineerId());
        if (engineer.getRole() != UserRole.ENGINEER) {
            throw new BadRequestException("Target user is not an Engineer.");
        }
        validateAssignee(engineer, complaint.getDistrict());

        complaint.setAssignedTo(engineer);
        complaint.setStatus(ComplaintStatus.ESCALATED_TO_ENGINEER);
        complaintRepository.save(complaint);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.ESCALATED_TO_ENGINEER)
                .fromStatus(ComplaintStatus.IN_PROGRESS).toStatus(ComplaintStatus.ESCALATED_TO_ENGINEER)
                .notes("Escalated to Engineer: " + engineer.getFullName()
                        + (request.notes() != null ? " — " + request.notes() : ""))
                .build());

        emailService.sendComplaintEscalatedEmail(
                engineer.getEmail(),
                engineer.getFullName(),
                complaint.getRefNumber(),
                complaint.getCustomerName(),
                complaint.getDistrict().getName(),
                currentUser.getFullName(),
                request.notes()
        );

        return ComplaintResponse.from(reload(complaintId));
    }

    // ── Mark Resolved ────────────────────────────────────────────

    @Transactional
    public ComplaintResponse markResolved(Long complaintId, MarkResolvedRequest request,
                                          UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        validateIsAssignee(complaint, currentUser);

        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS
                && complaint.getStatus() != ComplaintStatus.ESCALATED_TO_ENGINEER) {
            throw new BadRequestException("Complaint must be IN_PROGRESS or ESCALATED to mark as resolved.");
        }

        // Must have both analysis and solution
        if (analysisEntryRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).isEmpty()) {
            throw new BadRequestException("Analysis must be provided before marking as resolved.");
        }
        if (solutionEntryRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).isEmpty()) {
            throw new BadRequestException("Solution must be provided before marking as resolved.");
        }

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setCustomerFeedbackTaken(request.customerFeedbackTaken());
        complaint.setResolvedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.MARKED_RESOLVED)
                .fromStatus(oldStatus).toStatus(ComplaintStatus.RESOLVED)
                .notes((request.customerFeedbackTaken() ? "Customer feedback taken. " : "")
                        + (request.notes() != null ? request.notes() : ""))
                .build());

        // Notify managers responsible for this complaint's region
        Long regionId = complaint.getDistrict().getRegion().getId();
        userRepository.findActiveManagersByRegionId(regionId).forEach(manager ->
                emailService.sendResolutionPendingClosureEmail(
                        manager.getEmail(),
                        manager.getFullName(),
                        complaint.getRefNumber(),
                        complaint.getCustomerName(),
                        complaint.getDistrict().getName(),
                        currentUser.getFullName(),
                        complaint.getIssueCategory() != null
                                ? complaint.getIssueCategory().name() : "—"
                ));

        return toDetailResponse(reload(complaintId));

    }

    // ── Manager: Close / Reopen ──────────────────────────────────

    @Transactional
    public ComplaintResponse closeComplaint(Long complaintId, ManagerCloseRequest request,
                                            UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        if (currentUser.getRole() != UserRole.MANAGER) {
            throw new ForbiddenException("Only Managers can close complaints.");
        }
        validateManagerForComplaint(currentUser, complaint);

        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new BadRequestException("Only RESOLVED complaints can be closed.");
        }

        complaint.setStatus(ComplaintStatus.CLOSED);
        complaint.setClosedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.CLOSED)
                .fromStatus(ComplaintStatus.RESOLVED).toStatus(ComplaintStatus.CLOSED)
                .notes(request.notes())
                .build());

//        return ComplaintResponse.from(reload(complaintId));
        return toDetailResponse(reload(complaintId));

    }

    @Transactional
    public ComplaintResponse reopenComplaint(Long complaintId, ManagerReopenRequest request,
                                             UserDetailsImpl actor) {
        Complaint complaint = getComplaint(complaintId);
        User currentUser = getUser(actor.getId());

        if (currentUser.getRole() != UserRole.MANAGER) {
            throw new ForbiddenException("Only Managers can re-open complaints.");
        }
        validateManagerForComplaint(currentUser, complaint);

        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new BadRequestException("Only RESOLVED complaints can be re-opened.");
        }

        User assignee = getUser(request.assignedToId());
        validateAssignee(assignee, complaint.getDistrict());

        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setAssignedTo(assignee);
        complaint.setResolvedAt(null);
        complaintRepository.save(complaint);

        auditLogRepository.save(AuditLog.builder()
                .complaint(complaint).actor(currentUser)
                .action(AuditAction.REOPENED)
                .fromStatus(ComplaintStatus.RESOLVED).toStatus(ComplaintStatus.IN_PROGRESS)
                .notes("Re-assigned to " + assignee.getFullName() + ". Reason: " + request.notes())
                .build());

        emailService.sendComplaintReopenedEmail(
                assignee.getEmail(),
                assignee.getFullName(),
                complaint.getRefNumber(),
                complaint.getCustomerName(),
                complaint.getDistrict().getName(),
                currentUser.getFullName(),
                request.notes()
        );

        return toDetailResponse(reload(complaintId));

    }

    // ── Queries ──────────────────────────────────────────────────

    public ComplaintResponse getComplaintDetail(Long complaintId) {
        return toDetailResponse(reload(complaintId));

    }

    public ComplaintResponse getComplaintDetailByRef(String refNumber) {
        return toDetailResponse(reloadByRef(refNumber));

    }

    public List<String> getRaiserSuggestions(String q) {
        return complaintRepository.findRaiserSuggestions(q);
    }

    public Page<ComplaintResponse> getComplaintsForUser(
            UserDetailsImpl actor,
            String statusStr,
            String search,
            Pageable pageable) {

        User user = getUser(actor.getId());

        // Parse status — null means "all"
        ComplaintStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ComplaintStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Normalize search
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;

        return switch (user.getRole()) {
            case TECHNICAL_OFFICER, ENGINEER -> complaintRepository.findAllInUserDistrictsFiltered(
                            user.getId(), status, searchTerm, pageable)
                    .map(ComplaintResponse::summary);
            case MANAGER -> complaintRepository.findByRegionFiltered(
                            user.getRegion().getId(), status, searchTerm, pageable)
                    .map(ComplaintResponse::summary);
            case HEAD, ADMIN -> complaintRepository.findAllFiltered(status, searchTerm, pageable)
                    .map(ComplaintResponse::summary);
        };
    }

    // ── Ref Number Generation ────────────────────────────────────

    @Transactional
    public synchronized String generateRefNumber(District district, LocalDateTime now) {
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        ComplaintRefSequence seq = refSeqRepository
                .findByDistrictIdAndYearMonthLocked(district.getId(), yearMonth)
                .orElseGet(() -> ComplaintRefSequence.builder()
                        .districtId(district.getId())
                        .yearMonth(yearMonth)
                        .lastSeq(0)
                        .build());

        seq.setLastSeq(seq.getLastSeq() + 1);
        refSeqRepository.save(seq);

        return "%s-%s-%04d".formatted(district.getCode(), yearMonth, seq.getLastSeq());
    }

    public Page<ComplaintResponse> getMyQueue(UserDetailsImpl actor, Pageable pageable) {
        User user = getUser(actor.getId());
        return switch (user.getRole()) {
            case TECHNICAL_OFFICER, ENGINEER ->
                // Dashboard: only complaints assigned to this user
                    complaintRepository.findRelevantForUser(user.getId(), pageable)
                            .map(ComplaintResponse::summary);
            case MANAGER -> complaintRepository.findByRegionIdAndStatus(
                            user.getRegion().getId(), ComplaintStatus.IN_PROGRESS, pageable)
                    .map(ComplaintResponse::summary);
            case HEAD, ADMIN -> complaintRepository.findAll(pageable).map(ComplaintResponse::summary);
        };
    }

    // ── Validation Helpers ───────────────────────────────────────

    private void validateIsAssignee(Complaint complaint, User user) {
        if (complaint.getAssignedTo() == null
                || !complaint.getAssignedTo().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not the assigned owner of this complaint.");
        }
    }

    /**
     * For adding analysis/solution entries: the current assignee is always allowed.
     * Additionally, when the complaint is IN_PROGRESS or ESCALATED_TO_ENGINEER, any
     * active Technical Officer or Engineer belonging to the complaint's district may
     * also contribute.
     */
    private void validateCanContribute(Complaint complaint, User user) {
        // Assignee always allowed (covers all active statuses)
        if (complaint.getAssignedTo() != null
                && complaint.getAssignedTo().getId().equals(user.getId())) {
            return;
        }

        // For in-progress complaints, any TO/Engineer in the district may contribute
        ComplaintStatus status = complaint.getStatus();
        if (status == ComplaintStatus.IN_PROGRESS
                || status == ComplaintStatus.ESCALATED_TO_ENGINEER) {
            if (user.getRole() != UserRole.TECHNICAL_OFFICER
                    && user.getRole() != UserRole.ENGINEER) {
                throw new ForbiddenException("Only Technical Officers or Engineers can contribute to this complaint.");
            }
            if (!user.isActive()) {
                throw new ForbiddenException("Your account is inactive.");
            }
            boolean isInDistrict = user.getDistricts().stream()
                    .anyMatch(d -> d.getId().equals(complaint.getDistrict().getId()));
            if (!isInDistrict) {
                throw new ForbiddenException(
                        "You are not assigned to the district of this complaint.");
            }
            return;
        }

        throw new ForbiddenException("You are not the assigned owner of this complaint.");
    }

    private void validateAssignee(User assignee, District district) {
        if (assignee.getRole() != UserRole.TECHNICAL_OFFICER && assignee.getRole() != UserRole.ENGINEER) {
            throw new BadRequestException("Can only assign complaint to a Technical Officer or Engineer.");
        }
        if (!assignee.isActive()) {
            throw new BadRequestException("Cannot assign to an inactive user.");
        }
        boolean isInDistrict = assignee.getDistricts().stream()
                .anyMatch(d -> d.getId().equals(district.getId()));
        if (!isInDistrict) {
            throw new BadRequestException(
                    "Assignee " + assignee.getFullName() + " is not assigned to district " + district.getName());
        }
    }

    private void validateUserCanActOnDistrict(User user, District district) {
        if (user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.HEAD) {
            // Managers/Heads can create in their region
            return;
        }
        if (user.getRole() == UserRole.ADMIN) return;
        boolean isInDistrict = user.getDistricts().stream()
                .anyMatch(d -> d.getId().equals(district.getId()));
        if (!isInDistrict) {
            throw new ForbiddenException("You are not assigned to district: " + district.getName());
        }
    }

    private void validateManagerForComplaint(User manager, Complaint complaint) {
        if (manager.getRegion() == null) {
            throw new ForbiddenException("Manager has no region assigned.");
        }
        District district = complaint.getDistrict();
        if (district.getRegion() == null
                || !district.getRegion().getId().equals(manager.getRegion().getId())) {
            throw new ForbiddenException("This complaint is not in your region.");
        }
    }

    private void validateStatusForAnalysis(Complaint complaint) {
        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS
                && complaint.getStatus() != ComplaintStatus.ESCALATED_TO_ENGINEER) {
            throw new BadRequestException("Analysis can only be added when complaint is IN_PROGRESS or ESCALATED.");
        }
    }

    private void validateStatusForSolution(Complaint complaint) {
        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS
                && complaint.getStatus() != ComplaintStatus.ESCALATED_TO_ENGINEER) {
            throw new BadRequestException("Solution can only be added when complaint is IN_PROGRESS or ESCALATED.");
        }
    }

    private Complaint getComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", id));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

//    private Complaint reload(Long id) {
//        return complaintRepository.findById(id).orElseThrow();
//    }

    private Complaint reload(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
    }

    private Complaint reloadByRef(String refNumber) {
        return complaintRepository.findByRefNumber(refNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
    }



    private ComplaintResponse toDetailResponse(Complaint c) {
        return ComplaintResponse.from(
                c,
                entryId -> attachmentService.listForEntry("ANALYSIS", entryId),
                entryId -> attachmentService.listForEntry("SOLUTION", entryId)
        );
    }
}
