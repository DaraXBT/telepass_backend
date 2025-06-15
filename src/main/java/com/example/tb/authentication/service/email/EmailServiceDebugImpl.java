package com.example.tb.authentication.service.email;

import com.example.tb.configuration.OtpConfig;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mail.debug.mode", havingValue = "true")
public class EmailServiceDebugImpl implements EmailService {
    
    private final OtpConfig otpConfig;

    @Override
    public void sendRegistrationInvitation(String email, String name, String msg) throws MessagingException, UnsupportedEncodingException {
        log.info("=== DEBUG MODE: Registration Invitation ===");
        log.info("To: {}", email);
        log.info("Name: {}", name);
        log.info("Message: {}", msg);
        log.info("=========================================");
    }

    @Override
    public void sendVerificationEmail(String email, String verificationUrl, String otp) throws MessagingException, UnsupportedEncodingException {
        log.info("=== DEBUG MODE: Verification Email ===");
        log.info("To: {}", email);
        log.info("Verification URL: {}", verificationUrl);
        log.info("OTP: {}", otp);
        log.info("Expiration: {} minutes", otpConfig.getExpirationTime() / 60);
        log.info("=====================================");
    }

    @Override
    public void sendOtpToEmail(String email, String otp) throws MessagingException, UnsupportedEncodingException {
        log.info("🔐 === DEBUG MODE: OTP EMAIL ===");
        log.info("📧 To: {}", email);
        log.info("🔢 OTP: {}", otp);
        log.info("⏰ Expires in: {} minutes", otpConfig.getExpirationTime() / 60);
        log.info("📝 Subject: {}", otpConfig.getEmail().getSubject());
        
        String content = otpConfig.getEmail().getTemplate()
            .replace("{otp}", otp)
            .replace("{expiration}", String.valueOf(otpConfig.getExpirationTime() / 60));
            
        log.info("📄 Email Content:");
        log.info("---START EMAIL---");
        log.info(content);
        log.info("---END EMAIL---");
        log.info("===============================");
    }    @Override
    public void sendPasswordResetEmail(String email, String resetUrl) throws MessagingException, UnsupportedEncodingException {
        log.info("=== DEBUG MODE: Password Reset Email ===");
        log.info("To: {}", email);
        log.info("Reset URL: {}", resetUrl);
        log.info("Subject: Password Reset - Telepass");
        log.info("==========================================");
    }    @Override
    public void sendQRCodeEmail(String email, String userName, com.example.tb.model.response.EventResponse event, byte[] qrCodeBytes) throws MessagingException, UnsupportedEncodingException {
        log.info("🎫 === DEBUG MODE: Enhanced QR Code Email ===");
        log.info("📧 To: {}", email);
        log.info("👤 User Name: {}", userName);
        log.info("🎉 Event Name: {}", event != null ? event.getName() : "Unknown");
        log.info("📝 Event Description: {}", event != null ? event.getDescription() : "No description");
        log.info("🏷️ Event Category: {}", event != null ? event.getCategory() : "General");
        log.info("📅 Start DateTime: {}", event != null && event.getStartDateTime() != null ? event.getStartDateTime() : "TBA");
        log.info("📅 End DateTime: {}", event != null && event.getEndDateTime() != null ? event.getEndDateTime() : "TBA");
        log.info("� Location: {}", event != null ? event.getLocation() : "TBA");
        log.info("👥 Capacity: {}", event != null ? event.getCapacity() : 0);
        log.info("✅ Registered: {}", event != null ? event.getRegistered() : 0);
        log.info("�📏 QR Code Size: {} bytes (Enhanced 500x500px square format)", qrCodeBytes.length);
        log.info("📝 Subject: Event Registration QR Code - Telepass");
        log.info("🎨 Email Format: Enhanced HTML with comprehensive event details");
        log.info("📱 QR Code Display: 400x400px with blue border and shadow");
        log.info("🖼️ QR Code Position: Centered with event information above");
        log.info("💌 Enhanced Email Features:");
        log.info("   • Complete event details section");
        log.info("   • Start/End datetime display");
        log.info("   • Clickable location links");
        log.info("   • Capacity and registration count");
        log.info("   • Professional event category badge");
        log.info("   • Registration status confirmation");
        log.info("   • Enhanced instructions and styling");
        log.info("=======================================");
    }

    @Override
    public void sendCheckInConfirmationEmail(String email, String userName, String eventId) throws MessagingException, UnsupportedEncodingException {
        log.info("=== DEBUG MODE: Check-in Confirmation Email ===");
        log.info("To: {}", email);
        log.info("User Name: {}", userName);
        log.info("Event ID: {}", eventId);
        log.info("Time: {}", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        log.info("============================================");
    }
}
