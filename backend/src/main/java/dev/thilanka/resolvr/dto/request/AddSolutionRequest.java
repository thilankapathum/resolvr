package dev.thilanka.resolvr.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AddSolutionRequest(
        @NotBlank String content,
        LocalDate solutionTargetDate,
        String remarks
) {}
