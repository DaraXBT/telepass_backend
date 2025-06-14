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
    }
}
