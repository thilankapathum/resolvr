package dev.thilanka.resolvr.repository;

import dev.thilanka.resolvr.model.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByName(String name);
    boolean existsByName(String name);
}
