package dev.thilanka.resolvr.dto.response;

import dev.thilanka.resolvr.enums.*;
import dev.thilanka.resolvr.model.entity.Complaint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public record ComplaintResponse(
        Long id,
        String refNumber,

        Long districtId,
        String districtName,
        String districtCode,
        Long regionId,
        String regionName,

        ComplaintStatus status,
        ComplaintPriority priority,
        LocalDateTime targetDate,

        Long createdById,
        String createdByName,
        Long assignedToId,
        String assignedToName,
        String assignedToRole,
        String raisedBy,

        String customerName,
        String contactNumber,
        String msisdns,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        IssueCategory issueCategory,
        String issueDescription,
        String issueDuration,
        LocalDate lastExperienced,
        TechnologyType technology,
        String additionalInfo,

        DeviceType deviceType,
        Short signalBars,
        Boolean usingVpnApn,

        String servingSitesCells,
        String coverageQuality,

        boolean customerFeedbackTaken,

        List<AnalysisEntryResponse> analysisEntries,
        List<SolutionEntryResponse> solutionEntries,
        List<AuditLogResponse> auditLogs,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt
) {
    /**
     * Full detail view — includes analysis and solution entries with their attachments.
     *
     * @param attachmentsForEntry function that returns List<AttachmentResponse> for
     *                            a given (entryType, entryId) pair. Supplied by
     *                            ComplaintService which has access to AttachmentService.
     *                            e.g.: (type, id) -> attachmentService.listForEntry(type, id)
     */
    public static ComplaintResponse from(
            Complaint c,
            Function<Long, List<AttachmentResponse>> analysisAttachments,
            Function<Long, List<AttachmentResponse>> solutionAttachments) {

        return new ComplaintResponse(
                c.getId(), c.getRefNumber(),

                c.getDistrict().getId(), c.getDistrict().getName(), c.getDistrict().getCode(),
                c.getDistrict().getRegion() != null ? c.getDistrict().getRegion().getId()   : null,
                c.getDistrict().getRegion() != null ? c.getDistrict().getRegion().getName() : null,

                c.getStatus(), c.getPriority(), c.getTargetDate(),

                c.getCreatedBy().getId(), c.getCreatedBy().getFullName(),
                c.getAssignedTo() != null ? c.getAssignedTo().getId() : null,
                c.getAssignedTo() != null ? c.getAssignedTo().getFullName() : null,
                c.getAssignedTo() != null && c.getAssignedTo().getRole() != null
                        ? c.getAssignedTo().getRole().name() : null,
                c.getRaisedBy(),

                c.getCustomerName(), c.getContactNumber(), c.getMsisdns(),
                c.getAddress(), c.getLatitude(), c.getLongitude(),
                c.getIssueCategory(), c.getIssueDescription(),
                c.getIssueDuration(), c.getLastExperienced(),
                c.getTechnology(), c.getAdditionalInfo(),

                c.getDeviceType(), c.getSignalBars(), c.getUsingVpnApn(),

                c.getServingSitesCells(), c.getCoverageQuality(),

                c.isCustomerFeedbackTaken(),

                c.getAnalysisEntries().stream()
                        .map(e -> AnalysisEntryResponse.from(e, analysisAttachments.apply(e.getId())))
                        .toList(),
                c.getSolutionEntries().stream()
                        .map(e -> SolutionEntryResponse.from(e, solutionAttachments.apply(e.getId())))
                        .toList(),
                c.getAuditLogs().stream().map(AuditLogResponse::from).toList(),

                c.getCreatedAt(), c.getUpdatedAt(), c.getResolvedAt(), c.getClosedAt()
        );
    }

    /**
     * Convenience overload for backwards-compatibility — no attachments.
     * Used by list/summary views where attachments are not needed.
     */
    public static ComplaintResponse from(Complaint c) {
        return from(c, id -> List.of(), id -> List.of());
    }

    /** Summary version for list views — no threads, no attachments. */
    public static ComplaintResponse summary(Complaint c) {
        return new ComplaintResponse(
                c.getId(), c.getRefNumber(),
                c.getDistrict().getId(), c.getDistrict().getName(), c.getDistrict().getCode(),
                c.getDistrict().getRegion() != null ? c.getDistrict().getRegion().getId()   : null,
                c.getDistrict().getRegion() != null ? c.getDistrict().getRegion().getName() : null,
                c.getStatus(), c.getPriority(), c.getTargetDate(),
                c.getCreatedBy().getId(), c.getCreatedBy().getFullName(),
                c.getAssignedTo() != null ? c.getAssignedTo().getId() : null,
                c.getAssignedTo() != null ? c.getAssignedTo().getFullName() : null,
                c.getAssignedTo() != null && c.getAssignedTo().getRole() != null
                        ? c.getAssignedTo().getRole().name() : null,
                c.getRaisedBy(),
                c.getCustomerName(), c.getContactNumber(), c.getMsisdns(),
                null, c.getLatitude(), c.getLongitude(),
                c.getIssueCategory(), c.getIssueDescription(),
                null, null, c.getTechnology(), null,
                null, null, null,
                null, null,
                c.isCustomerFeedbackTaken(),
                List.of(), List.of(), List.of(),
                c.getCreatedAt(), c.getUpdatedAt(), c.getResolvedAt(), c.getClosedAt()
        );
    }
}