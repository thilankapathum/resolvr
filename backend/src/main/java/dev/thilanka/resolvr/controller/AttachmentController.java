package dev.thilanka.resolvr.controller;

import dev.thilanka.resolvr.dto.response.AttachmentResponse;
import dev.thilanka.resolvr.security.UserDetailsImpl;
import dev.thilanka.resolvr.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Attachment endpoints.
 *
 * Upload:  POST   /complaints/{complaintId}/analysis/{entryId}/attachments
 *          POST   /complaints/{complaintId}/solution/{entryId}/attachments
 *
 * List:    GET    /complaints/{complaintId}/analysis/{entryId}/attachments
 *          GET    /complaints/{complaintId}/solution/{entryId}/attachments
 *
 * Delete:  DELETE /attachments/{id}
 *
 * There is no "serve" endpoint here. Each AttachmentResponse already contains
 * a time-limited presigned MinIO URL that the client uses directly.
 * This is more efficient than proxying file bytes through the backend.
 */
@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    // ── Upload ─────────────────────────────────────────────────

    @PostMapping("/complaints/{complaintId}/analysis/{entryId}/attachments")
    public ResponseEntity<AttachmentResponse> uploadForAnalysis(
            @PathVariable Long complaintId,
            @PathVariable Long entryId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload("ANALYSIS", entryId, complaintId, file, currentUser));
    }

    @PostMapping("/complaints/{complaintId}/solution/{entryId}/attachments")
    public ResponseEntity<AttachmentResponse> uploadForSolution(
            @PathVariable Long complaintId,
            @PathVariable Long entryId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload("SOLUTION", entryId, complaintId, file, currentUser));
    }

    // ── List ───────────────────────────────────────────────────

    @GetMapping("/complaints/{complaintId}/analysis/{entryId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listForAnalysis(
            @PathVariable Long complaintId,
            @PathVariable Long entryId) {
        return ResponseEntity.ok(attachmentService.listForEntry("ANALYSIS", entryId));
    }

    @GetMapping("/complaints/{complaintId}/solution/{entryId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listForSolution(
            @PathVariable Long complaintId,
            @PathVariable Long entryId) {
        return ResponseEntity.ok(attachmentService.listForEntry("SOLUTION", entryId));
    }

    // ── Delete ─────────────────────────────────────────────────

    @DeleteMapping("/attachments/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        attachmentService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}