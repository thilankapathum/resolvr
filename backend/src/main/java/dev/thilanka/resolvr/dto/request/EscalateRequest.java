package dev.thilanka.resolvr.dto.request;

import jakarta.validation.constraints.NotNull;

public record EscalateRequest(
        @NotNull Long engineerId,
        String notes
) {}
