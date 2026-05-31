package dev.thilanka.resolvr.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManagerReopenRequest(
        @NotNull Long assignedToId,
        @NotBlank String notes
) {}
