package dev.thilanka.resolvr.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignComplaintRequest(@NotNull Long assignedToId) {}
