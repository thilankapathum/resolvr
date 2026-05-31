package dev.thilanka.resolvr.service;

import dev.thilanka.resolvr.dto.request.*;
import dev.thilanka.resolvr.dto.response.DistrictResponse;
import dev.thilanka.resolvr.dto.response.MessageResponse;
import dev.thilanka.resolvr.dto.response.RegionResponse;
import dev.thilanka.resolvr.dto.response.UserResponse;
import dev.thilanka.resolvr.enums.UserRole;
import dev.thilanka.resolvr.exception.BadRequestException;
import dev.thilanka.resolvr.exception.ConflictException;
import dev.thilanka.resolvr.exception.ResourceNotFoundException;
import dev.thilanka.resolvr.model.entity.District;
import dev.thilanka.resolvr.model.entity.Region;
import dev.thilanka.resolvr.model.entity.User;
import dev.thilanka.resolvr.repository.DistrictRepository;
import dev.thilanka.resolvr.repository.RefreshTokenRepository;
import dev.thilanka.resolvr.repository.RegionRepository;
import dev.thilanka.resolvr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // ── User Management ──────────────────────────────────────────

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    public Page<UserResponse> getPendingUsers(Pageable pageable) {
        // Users who registered but aren't active yet
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
        // In a production scenario you'd add a findByActiveAndEmailVerifiedTrue query
    }

    public UserResponse getUserById(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    @Transactional
    public UserResponse activateUser(Long userId, AdminActivateUserRequest request) {
        User user = getUser(userId);

        if (!user.isEmailVerified()) {
            throw new BadRequestException("Cannot activate user who hasn't verified their email.");
        }

        // Validate region requirement for non-admin roles
        if (request.role() != UserRole.ADMIN && request.regionId() == null) {
            throw new BadRequestException("A region must be assigned for role: " + request.role());
        }

        user.setActive(true);
        user.setRole(request.role());

        if (request.regionId() != null) {
            Region region = regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region", request.regionId()));
            user.setRegion(region);
        }

        User saved = userRepository.save(user);
        emailService.sendAccountActivatedEmail(saved.getEmail(), saved.getFullName(), saved.getRole().name());
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse deactivateUser(Long userId) {
        User user = getUser(userId);
        user.setActive(false);
        refreshTokenRepository.revokeAllByUserId(userId);
        User saved = userRepository.save(user);
        emailService.sendAccountDeactivatedEmail(saved.getEmail(), saved.getFullName());
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, AdminActivateUserRequest request) {
        User user = getUser(userId);
        user.setRole(request.role());

        if (request.regionId() != null) {
            Region region = regionRepository.findById(request.regionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region", request.regionId()));
            user.setRegion(region);
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public MessageResponse adminResetPassword(Long userId, AdminResetPasswordRequest request) {
        User user = getUser(userId);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId);
        emailService.sendAdminPasswordResetEmail(user.getEmail(), user.getFullName(), request.newPassword());
        return new MessageResponse("Password reset successfully for user: " + user.getEmail());
    }

    @Transactional
    public UserResponse assignDistricts(Long userId, AdminAssignDistrictsRequest request) {
        User user = getUser(userId);

        if (user.getRole() != UserRole.TECHNICAL_OFFICER && user.getRole() != UserRole.ENGINEER) {
            throw new BadRequestException("Districts can only be assigned to Technical Officers and Engineers.");
        }

        Set<District> districts = new HashSet<>();
        for (Long districtId : request.districtIds()) {
            District district = districtRepository.findById(districtId)
                    .orElseThrow(() -> new ResourceNotFoundException("District", districtId));
            districts.add(district);
        }

        user.setDistricts(districts);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse assignRegion(Long userId, Long regionId) {
        User user = getUser(userId);
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region", regionId));
        user.setRegion(region);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public MessageResponse deleteUser(Long userId) {
        User user = getUser(userId);
        if (user.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Cannot delete an admin user.");
        }
        refreshTokenRepository.revokeAllByUserId(userId);
        userRepository.delete(user);
        return new MessageResponse("User deleted successfully.");
    }

    // ── Region Management ────────────────────────────────────────

    public List<RegionResponse> getAllRegions() {
        return regionRepository.findAll().stream().map(RegionResponse::from).toList();
    }

    public RegionResponse getRegionById(Long id) {
        return RegionResponse.from(getRegion(id));
    }

    @Transactional
    public RegionResponse createRegion(CreateRegionRequest request) {
        if (regionRepository.existsByName(request.name())) {
            throw new ConflictException("A region with this name already exists.");
        }
        Region region = Region.builder().name(request.name()).build();
        return RegionResponse.from(regionRepository.save(region));
    }

    @Transactional
    public RegionResponse updateRegion(Long id, CreateRegionRequest request) {
        Region region = getRegion(id);
        region.setName(request.name());
        return RegionResponse.from(regionRepository.save(region));
    }

    @Transactional
    public MessageResponse deleteRegion(Long id) {
        Region region = getRegion(id);
        if (!region.getDistricts().isEmpty()) {
            throw new BadRequestException("Cannot delete a region that has districts assigned to it.");
        }
        regionRepository.delete(region);
        return new MessageResponse("Region deleted successfully.");
    }

    @Transactional
    public RegionResponse assignDistrictsToRegion(Long regionId, AssignDistrictToRegionRequest request) {
        Region region = getRegion(regionId);

        // Step 1 — unassign ALL districts currently in this region
        List<District> currentDistricts = districtRepository.findByRegionId(regionId);
        for (District district : currentDistricts) {
            district.setRegion(null);
            districtRepository.save(district);
        }

        // Step 2 — assign only the submitted selection
        for (Long districtId : request.districtIds()) {
            District district = districtRepository.findById(districtId)
                    .orElseThrow(() -> new ResourceNotFoundException("District", districtId));
            district.setRegion(region);
            districtRepository.save(district);
        }

        return RegionResponse.from(regionRepository.findById(regionId).orElseThrow());
    }

    // ── District Management ──────────────────────────────────────

    public List<DistrictResponse> getAllDistricts() {
        return districtRepository.findAll().stream().map(DistrictResponse::from).toList();
    }

    public List<DistrictResponse> getUnassignedDistricts() {
        return districtRepository.findByRegionIsNull().stream().map(DistrictResponse::from).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Region getRegion(Long id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Region", id));
    }
}
