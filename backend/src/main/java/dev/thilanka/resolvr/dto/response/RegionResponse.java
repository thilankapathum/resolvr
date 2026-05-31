package dev.thilanka.resolvr.dto.response;

import dev.thilanka.resolvr.model.entity.Region;

import java.util.List;

public record RegionResponse(Long id, String name, List<DistrictResponse> districts) {
    public static RegionResponse from(Region r) {
        return new RegionResponse(
                r.getId(), r.getName(),
                r.getDistricts().stream().map(DistrictResponse::from).toList()
        );
    }
}
