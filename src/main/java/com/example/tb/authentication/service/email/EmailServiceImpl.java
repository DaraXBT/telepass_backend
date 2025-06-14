package com.example.tb.authentication.service.email;

import com.example.tb.configuration.OtpConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mail.debug.mode", havingValue = "false", matchIfMissing = true)
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    private final OtpConfig otpConfig;
    
    @Value("${WebBaseUrl:http://localhost:3000/}")
    private String webBaseUrl;
    
    @Value("${email.from.address:noreply@telepass.com}")
    private String fromAddress;
    
    @Value("${email.from.name:Telepass Team}")
    private String fromName;

    @Override
    public void sendRegistrationInvitation(String email, String name, String msg) throws MessagingException, UnsupportedEncodingException {        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject("Registration Invitation - Telepass");
            
            String content = String.format(
                "Dear %s,\n\n%s\n\nPlease visit %s to complete your registration.\n\nBest regards,\nTelepass Team",
                name, msg, webBaseUrl
            );
              helper.setText(content);
            mailSender.send(message);
            
            log.info("Registration invitation sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send registration invitation to: {}", email, e);
            throw e;
        }
    }

    @Override
    public void sendVerificationEmail(String email, String verificationUrl, String otp) throws MessagingException, UnsupportedEncodingException {        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject("Email Verification - Telepass");
            
            String content = String.format(
                "Dear User,\n\n" +
                "Please verify your email address using either method below:\n\n" +
                "Method 1: Click the verification link:\n%s\n\n" +
                "Method 2: Use this verification code: %s\n\n" +
                "This code will expire in %d minutes.\n\n" +
                "If you didn't request this verification, please ignore this email.\n\n" +
                "Best regards,\nTelepass Team",
                verificationUrl, otp, otpConfig.getExpirationTime() / 60
            );
            
            helper.setText(content);
            mailSender.send(message);
            
            log.info("Verification email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
            throw e;
        }
    }

    @Override
    public void sendOtpToEmail(String email, String otp) throws MessagingException, UnsupportedEncodingException {        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject(otpConfig.getEmail().getSubject());
            
            String content = otpConfig.getEmail().getTemplate()
                .replace("{otp}", otp)
                .replace("{expiration}", String.valueOf(otpConfig.getExpirationTime() / 60));
            
            helper.setText(content);
            mailSender.send(message);
            
            log.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            throw e;
        }
    }
}
