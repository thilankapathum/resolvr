package dev.thilanka.resolvr.dto.request;

import dev.thilanka.resolvr.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record AdminActivateUserRequest(
        @NotNull UserRole role,
        Long regionId
) {}
