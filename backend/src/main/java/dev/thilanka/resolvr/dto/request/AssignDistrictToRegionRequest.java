package dev.thilanka.resolvr.dto.request;

import java.util.List;

public record AssignDistrictToRegionRequest(List<Long> districtIds) {
    public AssignDistrictToRegionRequest {
        // Default to empty list if null
        if (districtIds == null) districtIds = List.of();
    }
}