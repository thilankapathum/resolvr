package dev.thilanka.resolvr.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
