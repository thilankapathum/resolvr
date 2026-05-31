package dev.thilanka.resolvr.dto.response;

import dev.thilanka.resolvr.enums.*;
import dev.thilanka.resolvr.model.entity.Complaint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ComplaintResponse(
        Long id,
        String refNumber,

        // District / Region
        Long districtId,
        String districtName,
        String districtCode,
        Long regionId,
        String regionName,

        // Status & Priority
        ComplaintStatus status,
        ComplaintPriority priority,
        LocalDateTime targetDate,

        // Ownership
        Long createdById,
        String createdByName,
        Long assignedToId,
        String assignedToName,
        String assignedToRole,
        String raisedBy,

        // Customer
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

        // Device
        DeviceType deviceType,
        Short signalBars,
        Boolean usingVpnApn,

        // Analysis overview fields
        String servingSitesCells,
        String coverageQuality,

        // Customer feedback
        boolean customerFeedbackTaken,

        // Threads (for detail view)
        List<AnalysisEntryResponse> analysisEntries,
        List<SolutionEntryResponse> solutionEntries,
        List<AuditLogResponse> auditLogs,

        // Timestamps
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt
) {
    public static ComplaintResponse from(Complaint c) {
        return new ComplaintResponse(
                c.getId(), c.getRefNumber(),

                c.getDistrict().getId(),
                c.getDistrict().getName(),
                c.getDistrict().getCode(),
                c.getDistrict().getRegion() != null ? c.getDistrict().getRegion().getId() : null,
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

                c.getAnalysisEntries().stream().map(AnalysisEntryResponse::from).toList(),
                c.getSolutionEntries().stream().map(SolutionEntryResponse::from).toList(),
                c.getAuditLogs().stream().map(AuditLogResponse::from).toList(),

                c.getCreatedAt(), c.getUpdatedAt(), c.getResolvedAt(), c.getClosedAt()
        );
    }

    /** Summary version without threads — for list views */
    public static ComplaintResponse summary(Complaint c) {
        return new ComplaintResponse(
                c.getId(), c.getRefNumber(),
                c.getDistrict().getId(), c.getDistrict().getName(), c.getDistrict().getCode(),
                c.getDistrict().getRegion() != null ? c.getDistrict().getRegion().getId() : null,
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
