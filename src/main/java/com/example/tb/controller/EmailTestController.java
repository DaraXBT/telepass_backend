package com.example.tb.controller;

import com.example.tb.authentication.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Slf4j
@RequiredArgsConstructor
public class EmailTestController {

    private final JavaMailSender javaMailSender;
    private final EmailService emailService;

    @PostMapping("/email-connection")
    public ResponseEntity<Map<String, Object>> testEmailConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test the mail session
            javaMailSender.createMimeMessage();
            
            response.put("status", "SUCCESS");
            response.put("message", "SMTP connection successful");
            response.put("smtpHost", "smtp.gmail.com");
            response.put("smtpPort", "587");
            
            log.info("Email connection test successful");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "SMTP connection failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            log.error("Email connection test failed", e);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/send-test-email")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Send a simple test OTP email
            emailService.sendOtpToEmail(email, "123456");
            
            response.put("status", "SUCCESS");
            response.put("message", "Test email sent successfully to " + email);
            response.put("testOtp", "123456");
            
            log.info("Test email sent successfully to: {}", email);
            return ResponseEntity.ok(response);
            
        } catch (MessagingException | UnsupportedEncodingException e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to send test email: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            log.error("Failed to send test email to: {}", email, e);
            return ResponseEntity.badRequest().body(response);
        }
    }
}
