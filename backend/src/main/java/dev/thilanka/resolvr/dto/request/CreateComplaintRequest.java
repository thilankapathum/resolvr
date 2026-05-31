package dev.thilanka.resolvr.dto.request;

import dev.thilanka.resolvr.enums.ComplaintPriority;
import dev.thilanka.resolvr.enums.DeviceType;
import dev.thilanka.resolvr.enums.IssueCategory;
import dev.thilanka.resolvr.enums.TechnologyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateComplaintRequest(
        // General
        @NotNull Long districtId,
        @NotBlank String raisedBy,
        @NotNull ComplaintPriority priority,

        // Customer info
        @NotBlank @Size(max=150) String customerName,
        @NotBlank @Size(max=30) String contactNumber,
        @NotBlank String msisdns,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        @NotNull IssueCategory issueCategory,
        @NotBlank String issueDescription,
        String issueDuration,
        LocalDate lastExperienced,
        TechnologyType technology,
        String additionalInfo,

        // Device info
        DeviceType deviceType,
        Short signalBars,
        Boolean usingVpnApn,

        // Optional immediate assignment
        Long assignedToId
) {}
