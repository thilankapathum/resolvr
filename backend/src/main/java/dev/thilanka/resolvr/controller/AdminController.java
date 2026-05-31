package dev.thilanka.resolvr.controller;

import dev.thilanka.resolvr.dto.request.*;
import dev.thilanka.resolvr.dto.response.DistrictResponse;
import dev.thilanka.resolvr.dto.response.MessageResponse;
import dev.thilanka.resolvr.dto.response.RegionResponse;
import dev.thilanka.resolvr.dto.response.UserResponse;
import dev.thilanka.resolvr.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ── Users ────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PostMapping("/users/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminActivateUserRequest request) {
        return ResponseEntity.ok(adminService.activateUser(id, request));
    }

    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deactivateUser(id));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminActivateUserRequest request) {
        return ResponseEntity.ok(adminService.updateUserRole(id, request));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<MessageResponse> resetUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        return ResponseEntity.ok(adminService.adminResetPassword(id, request));
    }

    @PatchMapping("/users/{id}/districts")
    public ResponseEntity<UserResponse> assignDistricts(
            @PathVariable Long id,
            @Valid @RequestBody AdminAssignDistrictsRequest request) {
        return ResponseEntity.ok(adminService.assignDistricts(id, request));
    }

    @PatchMapping("/users/{id}/region/{regionId}")
    public ResponseEntity<UserResponse> assignRegion(
            @PathVariable Long id, @PathVariable Long regionId) {
        return ResponseEntity.ok(adminService.assignRegion(id, regionId));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteUser(id));
    }

    // ── Regions ──────────────────────────────────────────────────

    @GetMapping("/regions")
    public ResponseEntity<List<RegionResponse>> getAllRegions() {
        return ResponseEntity.ok(adminService.getAllRegions());
    }

    @GetMapping("/regions/{id}")
    public ResponseEntity<RegionResponse> getRegionById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getRegionById(id));
    }

    @PostMapping("/regions")
    public ResponseEntity<RegionResponse> createRegion(@Valid @RequestBody CreateRegionRequest request) {
        return ResponseEntity.ok(adminService.createRegion(request));
    }

    @PutMapping("/regions/{id}")
    public ResponseEntity<RegionResponse> updateRegion(
            @PathVariable Long id, @Valid @RequestBody CreateRegionRequest request) {
        return ResponseEntity.ok(adminService.updateRegion(id, request));
    }

    @DeleteMapping("/regions/{id}")
    public ResponseEntity<MessageResponse> deleteRegion(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteRegion(id));
    }

    @PostMapping("/regions/{regionId}/districts")
    public ResponseEntity<RegionResponse> assignDistrictsToRegion(
            @PathVariable Long regionId,
            @Valid @RequestBody AssignDistrictToRegionRequest request) {
        return ResponseEntity.ok(adminService.assignDistrictsToRegion(regionId, request));
    }

    // ── Districts ────────────────────────────────────────────────

    @GetMapping("/districts")
    public ResponseEntity<List<DistrictResponse>> getAllDistricts() {
        return ResponseEntity.ok(adminService.getAllDistricts());
    }

    @GetMapping("/districts/unassigned")
    public ResponseEntity<List<DistrictResponse>> getUnassignedDistricts() {
        return ResponseEntity.ok(adminService.getUnassignedDistricts());
    }
}
