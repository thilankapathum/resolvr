package dev.thilanka.resolvr.service;

import dev.thilanka.resolvr.dto.request.*;
import dev.thilanka.resolvr.dto.response.AuthResponse;
import dev.thilanka.resolvr.dto.response.MessageResponse;
import dev.thilanka.resolvr.dto.response.UserResponse;
import dev.thilanka.resolvr.exception.BadRequestException;
import dev.thilanka.resolvr.exception.ConflictException;
import dev.thilanka.resolvr.exception.ResourceNotFoundException;
import dev.thilanka.resolvr.model.entity.RefreshToken;
import dev.thilanka.resolvr.model.entity.User;
import dev.thilanka.resolvr.repository.RefreshTokenRepository;
import dev.thilanka.resolvr.repository.UserRepository;
import dev.thilanka.resolvr.security.JwtService;
import dev.thilanka.resolvr.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${resolvr.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Value("${resolvr.frontend-url}")
    private String frontendUrl;

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists.");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .active(false)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .build();

        userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationToken);

        return new MessageResponse(
                "Registration successful. Please check your email to verify your account. " +
                        "An administrator will activate your account after verification."
        );
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token."));

        if (user.isEmailVerified()) {
            return new MessageResponse("Email already verified.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return new MessageResponse("Email verified successfully. Please wait for an admin to activate your account.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // AuthenticationManager handles DisabledException for inactive accounts
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userDetails.getId()));

        if (!user.isEmailVerified()) {
            throw new BadRequestException("Please verify your email address before logging in.");
        }

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token."));

        if (!stored.isValid()) {
            throw new BadRequestException("Refresh token expired or revoked. Please log in again.");
        }

        // Rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String newAccess = jwtService.generateAccessToken(userDetails);
        String newRefresh = createRefreshToken(user);

        return new AuthResponse(newAccess, newRefresh, UserResponse.from(user));
    }

    @Transactional
    public MessageResponse logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
        return new MessageResponse("Logged out successfully.");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(2));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
        });
        // Always return the same message to prevent email enumeration
        return new MessageResponse("If that email is registered, a password reset link has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token."));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(user.getId());

        return new MessageResponse("Password reset successfully. Please log in with your new password.");
    }

    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        UserDetailsImpl currentUser = getCurrentUser();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens to force re-login on other devices
        refreshTokenRepository.revokeAllByUserId(user.getId());

        return new MessageResponse("Password changed successfully.");
    }

    public UserResponse getCurrentUserProfile() {
        UserDetailsImpl currentUser = getCurrentUser();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));
        return UserResponse.from(user);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String createRefreshToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiry(LocalDateTime.now().plusNanos(refreshTokenExpiryMs * 1_000_000L))
                .build();
        refreshTokenRepository.save(rt);
        return tokenValue;
    }

    public static UserDetailsImpl getCurrentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
