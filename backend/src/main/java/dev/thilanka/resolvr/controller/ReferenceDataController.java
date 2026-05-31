package dev.thilanka.resolvr.controller;

import dev.thilanka.resolvr.dto.response.DistrictResponse;
import dev.thilanka.resolvr.dto.response.RegionResponse;
import dev.thilanka.resolvr.dto.response.UserResponse;
import dev.thilanka.resolvr.enums.UserRole;
import dev.thilanka.resolvr.repository.DistrictRepository;
import dev.thilanka.resolvr.repository.RegionRepository;
import dev.thilanka.resolvr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ref")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReferenceDataController {

    private final DistrictRepository districtRepository;
    private final RegionRepository regionRepository;
    private final UserRepository userRepository;

    @GetMapping("/districts")
    public ResponseEntity<List<DistrictResponse>> getAllDistricts() {
        return ResponseEntity.ok(
                districtRepository.findAll().stream()
                        .map(DistrictResponse::from)
                        .toList()
        );
    }

    @GetMapping("/regions")
    public ResponseEntity<List<RegionResponse>> getAllRegions() {
        return ResponseEntity.ok(
                regionRepository.findAll().stream()
                        .map(RegionResponse::from)
                        .toList()
        );
    }

    // Engineers and TOs assignable to a specific district
    @GetMapping("/districts/{districtId}/assignable-users")
    public ResponseEntity<List<UserResponse>> getAssignableUsersForDistrict(
            @PathVariable Long districtId) {
        List<UserResponse> users = List.of(UserRole.TECHNICAL_OFFICER, UserRole.ENGINEER)
                .stream()
                .flatMap(role -> userRepository
                        .findActiveByDistrictAndRole(districtId, role).stream())
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    // All active TOs and Engineers (for forms that don't filter by district)
    @GetMapping("/assignable-users")
    public ResponseEntity<List<UserResponse>> getAllAssignableUsers() {
        List<UserResponse> users = List.of(UserRole.TECHNICAL_OFFICER, UserRole.ENGINEER)
                .stream()
                .flatMap(role -> userRepository.findByRoleAndActiveTrue(role).stream())
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }
}
