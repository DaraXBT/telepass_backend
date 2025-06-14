package com.example.tb.authentication.service.otp;

import com.example.tb.configuration.OtpConfig;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class OtpServiceImpl implements OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);
    
    private final OtpConfig otpConfig;
    private final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptData> attemptStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
      @Autowired
    public OtpServiceImpl(OtpConfig otpConfig) {
        this.otpConfig = otpConfig;
    }
    
    private static class OtpData {
        private final String otp;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        
        public OtpData(String otp, long expirationTimeSeconds) {
            this.otp = otp;
            this.createdAt = LocalDateTime.now();
            this.expiresAt = createdAt.plusSeconds(expirationTimeSeconds);
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
        
        public String getOtp() { return otp; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }
    
    private static class AttemptData {
        private int attempts;
        private LocalDateTime lastAttempt;
        private LocalDateTime cooldownUntil;
        
        public AttemptData() {
            this.attempts = 0;
            this.lastAttempt = LocalDateTime.now();
        }
        
        public void incrementAttempts(long cooldownTimeSeconds) {
            this.attempts++;
            this.lastAttempt = LocalDateTime.now();
            if (attempts >= 3) { // Max attempts reached
                this.cooldownUntil = lastAttempt.plusSeconds(cooldownTimeSeconds);
            }
        }
        
        public boolean isInCooldown() {
            return cooldownUntil != null && LocalDateTime.now().isBefore(cooldownUntil);
        }
        
        public void reset() {
            this.attempts = 0;
            this.cooldownUntil = null;
        }
        
        public int getAttempts() { return attempts; }
        public LocalDateTime getCooldownUntil() { return cooldownUntil; }
    }    @Override
    public String generateOtp(String username) {
        logger.info("Generating OTP for username: {}", username);
        
        // Check if user is in cooldown
        AttemptData attemptData = attemptStore.get(username);
        if (attemptData != null && attemptData.isInCooldown()) {
            logger.warn("OTP generation blocked for username: {} - cooldown until: {}", 
                       username, attemptData.getCooldownUntil());
            throw new RuntimeException("Too many failed attempts. Please wait before requesting a new OTP.");
        }
          // Generate OTP based on configured length
        int otpRange = (int) Math.pow(10, otpConfig.getLength());
        String otpFormat = "%0" + otpConfig.getLength() + "d";
        String otp = String.format(otpFormat, random.nextInt(otpRange));
        
        // Store OTP with expiration
        OtpData otpData = new OtpData(otp, otpConfig.getExpirationTime());
        otpStore.put(username, otpData);
        
        // Schedule cleanup
        scheduleCleanup(username, otpConfig.getExpirationTime());
        
        logger.info("OTP generated successfully for username: {}, expires at: {}", 
                   username, otpData.getExpiresAt());
        return otp;
    }
    @Override
    public String generateOtpEmail(String email) {
        logger.info("Generating OTP for email: {}", email);
        
        // Check if email is in cooldown
        AttemptData attemptData = attemptStore.get(email);
        if (attemptData != null && attemptData.isInCooldown()) {
            logger.warn("OTP generation blocked for email: {} - cooldown until: {}", 
                       email, attemptData.getCooldownUntil());
            throw new RuntimeException("Too many failed attempts. Please wait before requesting a new OTP.");
        }
          // Generate OTP based on configured length
        int otpRange = (int) Math.pow(10, otpConfig.getLength());
        String otpFormat = "%0" + otpConfig.getLength() + "d";
        String otp = String.format(otpFormat, random.nextInt(otpRange));
        
        // Store OTP with expiration
        OtpData otpData = new OtpData(otp, otpConfig.getExpirationTime());
        otpStore.put(email, otpData);
        
        // Schedule cleanup
        scheduleCleanup(email, otpConfig.getExpirationTime());
        
        logger.info("OTP generated successfully for email: {}, expires at: {}", 
                   email, otpData.getExpiresAt());
        return otp;
    }    @Override
    public boolean validateOtp(String username, String otp) {
        logger.debug("Validating OTP for username: {}", username);
        
        OtpData otpData = otpStore.get(username);
        if (otpData == null) {
            logger.warn("No OTP found for username: {}", username);
            return false;
        }
        
        if (otpData.isExpired()) {
            logger.warn("OTP expired for username: {}", username);
            otpStore.remove(username);
            return false;
        }
        
        boolean isValid = otp.equals(otpData.getOtp());
          if (!isValid) {
            // Track failed attempt
            AttemptData attemptData = attemptStore.computeIfAbsent(username, k -> new AttemptData());
            attemptData.incrementAttempts(otpConfig.getCooldownTime());
            
            logger.warn("Invalid OTP for username: {}, attempts: {}", username, attemptData.getAttempts());
            
            if (attemptData.getAttempts() >= otpConfig.getMaxAttempts()) {
                logger.warn("Max OTP attempts reached for username: {}, cooldown until: {}", 
                           username, attemptData.getCooldownUntil());
            }
        } else {
            // Reset attempts on successful validation
            attemptStore.remove(username);
            otpStore.remove(username);
            logger.info("OTP validated successfully for username: {}", username);
        }
        
        return isValid;
    }    @Override
    public boolean validateOtpEmail(String email, String otp) {
        logger.debug("Validating OTP for email: {}", email);
        
        OtpData otpData = otpStore.get(email);
        if (otpData == null) {
            logger.warn("No OTP found for email: {}", email);
            return false;
        }
        
        if (otpData.isExpired()) {
            logger.warn("OTP expired for email: {}", email);
            otpStore.remove(email);
            return false;
        }
        
        boolean isValid = otp.equals(otpData.getOtp());
          if (!isValid) {
            // Track failed attempt
            AttemptData attemptData = attemptStore.computeIfAbsent(email, k -> new AttemptData());
            attemptData.incrementAttempts(otpConfig.getCooldownTime());
            
            logger.warn("Invalid OTP for email: {}, attempts: {}", email, attemptData.getAttempts());
            
            if (attemptData.getAttempts() >= otpConfig.getMaxAttempts()) {
                logger.warn("Max OTP attempts reached for email: {}, cooldown until: {}", 
                           email, attemptData.getCooldownUntil());
            }
        } else {
            // Reset attempts on successful validation
            attemptStore.remove(email);
            otpStore.remove(email);
            logger.info("OTP validated successfully for email: {}", email);
        }
        
        return isValid;
    }    @Override
    public String generateEmailToken(String email) {
        logger.info("Generating email token for: {}", email);
        
        String randomNum = String.format("%06d", random.nextInt(1000000));
        String combined = email + ":" + randomNum;
        String token = Base64.encodeBase64URLSafeString(combined.getBytes(StandardCharsets.UTF_8));
          // Store token with expiration (using same expiration time as OTP)
        OtpData tokenData = new OtpData(email, otpConfig.getExpirationTime());
        otpStore.put(token, tokenData);
        
        // Schedule cleanup
        scheduleCleanup(token, otpConfig.getExpirationTime());
        
        logger.info("Email token generated successfully for: {}", email);
        return token;
    }

    public String decodeEmailToken(String token) {
        try {
            byte[] decodedBytes = Base64.decodeBase64(token);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            return decodedString.split(":")[0];
        } catch (Exception e) {
            logger.error("Error decoding email token: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateEmailToken(String token, String email) {
        logger.debug("Validating email token for: {}", email);
        
        OtpData tokenData = otpStore.get(token);
        if (tokenData == null) {
            logger.warn("No token found for validation");
            return false;
        }
        
        if (tokenData.isExpired()) {
            logger.warn("Token expired for email: {}", email);
            otpStore.remove(token);
            return false;
        }
        
        boolean isValid = tokenData.getOtp().equals(email);
        if (isValid) {
            otpStore.remove(token);
            logger.info("Email token validated successfully for: {}", email);
        } else {
            logger.warn("Invalid email token for: {}", email);
        }
        
        return isValid;
    }
      private void scheduleCleanup(String key, long delaySeconds) {
        executorService.schedule(() -> {
            otpStore.remove(key);
            attemptStore.remove(key);
            logger.debug("Cleaned up expired OTP/token for key: {}", key);
        }, delaySeconds, TimeUnit.SECONDS);
    }

    // DEBUG METHOD - REMOVE IN PRODUCTION
    @Override
    public String getCurrentOtpForDebug(String email) {
        String emailKey = "email:" + email.toLowerCase();
        OtpData otpData = otpStore.get(emailKey);
        
        if (otpData == null) {
            throw new IllegalArgumentException("No OTP found for email: " + email);
        }
        
        if (otpData.isExpired()) {
            otpStore.remove(emailKey);
            throw new IllegalArgumentException("OTP has expired for email: " + email);
        }
        
        logger.warn("🚨 DEBUG: Retrieved OTP {} for email {}", otpData.getOtp(), email);
        return otpData.getOtp();
    }
}
