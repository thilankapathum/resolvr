package dev.thilanka.resolvr.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddAnalysisRequest(
        @NotBlank String content,
        String servingSitesCells,
        String coverageQuality
) {}
