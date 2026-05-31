package dev.thilanka.resolvr.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AdminAssignDistrictsRequest(@NotNull Set<Long> districtIds) {}
