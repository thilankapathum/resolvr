package dev.thilanka.resolvr.dto.request;

import jakarta.validation.constraints.NotNull;

public record MarkResolvedRequest(
        @NotNull Boolean customerFeedbackTaken,
        String notes
) {}
