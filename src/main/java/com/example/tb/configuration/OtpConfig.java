package com.example.tb.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "otp")
public class OtpConfig {
    private int expirationTime = 300; // 5 minutes in seconds
    private int length = 6; // Number of digits in OTP
    private int maxAttempts = 3; // Maximum verification attempts
    private int cooldownTime = 60; // Cooldown time in seconds before requesting new OTP
    private Email email = new Email();

    @Data
    public static class Email {
        private String subject = "Your OTP Code";
        private String template = "Dear User,\n\nYour One-Time Password (OTP) is: {otp}\n\nThis code will expire in {expiration} minutes.\n\nIf you didn't request this code, please ignore this email.\n\nBest regards,\nTelepass Team";
    }
}
