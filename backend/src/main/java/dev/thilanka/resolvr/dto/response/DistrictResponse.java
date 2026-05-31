package dev.thilanka.resolvr.dto.response;

import dev.thilanka.resolvr.model.entity.District;

public record DistrictResponse(Long id, String name, String code, Long regionId, String regionName) {
    public static DistrictResponse from(District d) {
        return new DistrictResponse(
                d.getId(), d.getName(), d.getCode(),
                d.getRegion() != null ? d.getRegion().getId() : null,
                d.getRegion() != null ? d.getRegion().getName() : null
        );
    }
}
