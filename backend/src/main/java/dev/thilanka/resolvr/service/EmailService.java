package dev.thilanka.resolvr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${resolvr.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String to, String name, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        String body = """
            Hello %s,
            
            Thank you for registering with Resolvr.
            
            Please verify your email address by clicking the link below:
            %s
            
            This link does not expire.
            
            After email verification, an administrator will activate your account and assign your role.
            
            Regards,
            The Resolvr Team
            """.formatted(name, link);
        sendEmail(to, "Verify your Resolvr account", body);
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String body = """
            Hello %s,
            
            A password reset was requested for your Resolvr account.
            
            Click the link below to reset your password (valid for 2 hours):
            %s
            
            If you did not request this, please ignore this email.
            
            Regards,
            The Resolvr Team
            """.formatted(name, link);
        sendEmail(to, "Reset your Resolvr password", body);
    }

    @Async
    public void sendAccountActivatedEmail(String to, String name, String role) {
        String body = """
            Hello %s,
            
            Your Resolvr account has been activated by an administrator.
            
            Your assigned role: %s
            
            You can now log in at: %s/login
            
            Regards,
            The Resolvr Team
            """.formatted(name, role, frontendUrl);
        sendEmail(to, "Your Resolvr account is now active", body);
    }

    @Async
    public void sendAccountDeactivatedEmail(String to, String name) {
        String body = """
            Hello %s,
            
            Your Resolvr account has been deactivated by an administrator.
            Please contact your administrator if you believe this is an error.
            
            Regards,
            The Resolvr Team
            """.formatted(name);
        sendEmail(to, "Your Resolvr account has been deactivated", body);
    }

    @Async
    public void sendAdminPasswordResetEmail(String to, String name, String newPassword) {
        String body = """
            Hello %s,
            
            An administrator has reset your Resolvr password.
            
            Your temporary password: %s
            
            Please log in and change your password immediately.
            Login: %s/login
            
            Regards,
            The Resolvr Team
            """.formatted(name, newPassword, frontendUrl);
        sendEmail(to, "Your Resolvr password has been reset", body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
