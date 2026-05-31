package dev.thilanka.resolvr.dto.response;

import dev.thilanka.resolvr.enums.UserRole;
import dev.thilanka.resolvr.model.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        boolean active,
        boolean emailVerified,
        Long regionId,
        String regionName,
        Set<Long> districtIds,
        Set<String> districtNames,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.isEmailVerified(),
                user.getRegion() != null ? user.getRegion().getId() : null,
                user.getRegion() != null ? user.getRegion().getName() : null,
                user.getDistricts().stream().map(d -> d.getId()).collect(Collectors.toSet()),
                user.getDistricts().stream().map(d -> d.getName()).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}
