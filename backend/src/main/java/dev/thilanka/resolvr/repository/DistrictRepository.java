package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.model.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {
    Optional<District> findByCode(String code);
    List<District> findByRegionId(Long regionId);
    List<District> findByRegionIsNull();
}
