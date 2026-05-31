package dev.thilanka.resolvr.controller;

import dev.thilanka.resolvr.dto.request.*;
import dev.thilanka.resolvr.dto.response.ComplaintResponse;
import dev.thilanka.resolvr.security.UserDetailsImpl;
import dev.thilanka.resolvr.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    // ── List & Detail ────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<ComplaintResponse>> getComplaints(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(complaintService.getComplaintsForUser(currentUser, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaint(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintDetail(id));
    }

    // ── Create ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICAL_OFFICER','ENGINEER','MANAGER','HEAD','ADMIN')")
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complaintService.createComplaint(request, currentUser));
    }

    // ── Assign ───────────────────────────────────────────────────

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ENGINEER','MANAGER','HEAD','ADMIN')")
    public ResponseEntity<ComplaintResponse> assignComplaint(
            @PathVariable Long id,
            @Valid @RequestBody AssignComplaintRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.assignComplaint(id, request, currentUser));
    }

    // ── Start ────────────────────────────────────────────────────

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('TECHNICAL_OFFICER','ENGINEER')")
    public ResponseEntity<ComplaintResponse> startComplaint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.startComplaint(id, currentUser));
    }

    // ── Analysis ─────────────────────────────────────────────────

    @PostMapping("/{id}/analysis")
    @PreAuthorize("hasAnyRole('TECHNICAL_OFFICER','ENGINEER')")
    public ResponseEntity<ComplaintResponse> addAnalysis(
            @PathVariable Long id,
            @Valid @RequestBody AddAnalysisRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.addAnalysis(id, request, currentUser));
    }

    @PutMapping("/{id}/analysis/{entryId}")
    @PreAuthorize("hasAnyRole('TECHNICAL_OFFICER','ENGINEER')")
    public ResponseEntity<ComplaintResponse> editAnalysis(
            @PathVariable Long id,
            @PathVariable Long entryId,
            @Valid @RequestBody AddAnalysisRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.editAnalysis(id, entryId, request, currentUser));
    }

    // ── Solution ─────────────────────────────────────────────────

    @PostMapping("/{id}/solution")
    @PreAuthorize("hasAnyRole('TECHNICAL_OFFICER','ENGINEER')")
    public ResponseEntity<ComplaintResponse> addSolution(
            @PathVariable Long id,
            @Valid @RequestBody AddSolutionRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.addSolution(id, request, currentUser));
    }

    @PutMapping("/{id}/solution/{entryId}")
    @PreAuthorize("hasAnyRole('TECHNICAL_OFFICER','ENGINEER')")
    public ResponseEntity<ComplaintResponse> editSolution(
            @PathVariable Long id,
            @PathVariable Long entryId,
            @Valid @RequestBody AddSolutionRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.editSolution(id, entryId, request, currentUser));
    }

    // ── Escalate ─────────────────────────────────────────────────

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasRole('TECHNICAL_OFFICER')")
    public ResponseEntity<ComplaintResponse> escalate(
            @PathVariable Long id,
            @Valid @RequestBody EscalateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.escalateToEngineer(id, request, currentUser));
    }

    // ── Mark Resolved ─────────────────────────────────────────────

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('TECHNICAL_OFFICER','ENGINEER')")
    public ResponseEntity<ComplaintResponse> markResolved(
            @PathVariable Long id,
            @Valid @RequestBody MarkResolvedRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.markResolved(id, request, currentUser));
    }

    // ── Manager Actions ──────────────────────────────────────────

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ComplaintResponse> closeComplaint(
            @PathVariable Long id,
            @RequestBody(required = false) ManagerCloseRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.closeComplaint(
                id, request != null ? request : new ManagerCloseRequest(null), currentUser));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ComplaintResponse> reopenComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ManagerReopenRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(complaintService.reopenComplaint(id, request, currentUser));
    }
}
