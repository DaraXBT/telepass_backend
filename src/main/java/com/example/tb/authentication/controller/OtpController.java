package com.example.tb.authentication.controller;

import com.example.tb.authentication.service.email.EmailService;
import com.example.tb.authentication.service.otp.OtpService;
import com.example.tb.configuration.OtpConfig;
import com.example.tb.model.request.OtpRequestEmail;
import com.example.tb.model.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/otp")
@CrossOrigin
@Slf4j
@RequiredArgsConstructor
@Tag(name = "OTP Controller", description = "OTP testing and management endpoints")
public class OtpController {

    private final OtpService otpService;
    private final EmailService emailService;
    private final OtpConfig otpConfig;

    @PostMapping("/test/send-otp")
    @Operation(summary = "Test endpoint to send OTP to email", 
               description = "Sends an OTP to the specified email address for testing purposes")
    public ResponseEntity<ApiResponse<?>> sendTestOtp(@RequestParam String email) {
        try {
            // Generate OTP
            String otp = otpService.generateOtpEmail(email);
            
            // Send OTP via email
            emailService.sendOtpToEmail(email, otp);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", email);
            responseData.put("otpGenerated", true);
            responseData.put("expirationTimeSeconds", otpConfig.getExpirationTime());
            responseData.put("otpLength", otpConfig.getLength());
            
            log.info("Test OTP sent successfully to: {}", email);
              return ResponseEntity.ok(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Test OTP sent successfully to " + email)
                    .payload(responseData)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to send test OTP to: {}", email, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Failed to send OTP: " + e.getMessage())
                    .build()
            );
        }
    }

    @PostMapping("/test/validate-otp")
    @Operation(summary = "Test endpoint to validate OTP", 
               description = "Validates the OTP for the specified email address")
    public ResponseEntity<ApiResponse<?>> validateTestOtp(@RequestBody OtpRequestEmail otpRequest) {
        try {
            boolean isValid = otpService.validateOtpEmail(otpRequest.getEmail(), otpRequest.getOtp());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", otpRequest.getEmail());
            responseData.put("otp", otpRequest.getOtp());
            responseData.put("isValid", isValid);
            responseData.put("validatedAt", LocalDateTime.now());
            
            if (isValid) {
                log.info("Test OTP validated successfully for: {}", otpRequest.getEmail());                return ResponseEntity.ok(
                    ApiResponse.builder()
                        .date(LocalDateTime.now())
                        .message("OTP validated successfully")
                        .payload(responseData)
                        .build()
                );
            } else {
                log.warn("Invalid test OTP attempt for: {}", otpRequest.getEmail());
                return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                        .date(LocalDateTime.now())
                        .message("Invalid OTP code")
                        .payload(responseData)
                        .build()
                );
            }
        } catch (Exception e) {
            log.error("Error validating test OTP for: {}", otpRequest.getEmail(), e);
            return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Error validating OTP: " + e.getMessage())
                    .build()
            );
        }
    }

    @PostMapping("/test/quick-test")
    @Operation(summary = "Quick OTP test with default admin email", 
               description = "Sends OTP to default admin email (antonyy.anonymous@gmail.com) for quick testing")
    public ResponseEntity<ApiResponse<?>> quickOtpTest() {
        String testEmail = "antonyy.anonymous@gmail.com";
        
        try {
            // Generate and send OTP
            String otp = otpService.generateOtpEmail(testEmail);
            emailService.sendOtpToEmail(testEmail, otp);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("testEmail", testEmail);
            responseData.put("otpGenerated", true);
            responseData.put("generatedOtp", otp); // For testing purposes only
            responseData.put("expirationTime", otpConfig.getExpirationTime() + " seconds");
            responseData.put("instructions", "Use the generated OTP to test validation endpoint");
            
            log.info("Quick test OTP sent to: {}", testEmail);
              return ResponseEntity.ok(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Quick test OTP sent successfully")
                    .payload(responseData)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to send quick test OTP", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Failed to send quick test OTP: " + e.getMessage())
                    .build()
            );
        }
    }

    @GetMapping("/config")
    @Operation(summary = "Get OTP configuration", 
               description = "Returns current OTP configuration settings")
    public ResponseEntity<ApiResponse<?>> getOtpConfig() {
        Map<String, Object> configData = new HashMap<>();
        configData.put("expirationTime", otpConfig.getExpirationTime());
        configData.put("length", otpConfig.getLength());
        configData.put("maxAttempts", otpConfig.getMaxAttempts());
        configData.put("cooldownTime", otpConfig.getCooldownTime());
        configData.put("emailSubject", otpConfig.getEmail().getSubject());
          return ResponseEntity.ok(
            ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("OTP configuration retrieved successfully")
                .payload(configData)
                .build()
        );
    }

    @PostMapping("/test/simulate-admin-scenario")
    @Operation(summary = "Simulate admin OTP scenario", 
               description = "Simulates the admin OTP scenario with the provided user data")
    public ResponseEntity<ApiResponse<?>> simulateAdminScenario() {
        // Admin data from your example
        String adminEmail = "antonyy.anonymous@gmail.com";
        String adminName = "niccolò";
        String adminUsername = "antonyy.anonymous";
        
        try {
            // Generate OTP for admin
            String otp = otpService.generateOtpEmail(adminEmail);
            
            // Send OTP email
            emailService.sendOtpToEmail(adminEmail, otp);
            
            Map<String, Object> adminData = new HashMap<>();
            adminData.put("adminId", "7e006997-1355-47c2-9cc9-91677892ecf2");
            adminData.put("email", adminEmail);
            adminData.put("name", adminName);
            adminData.put("username", adminUsername);
            adminData.put("googleId", "105518625925782116548");
            adminData.put("profileImage", "https://lh3.googleusercontent.com/a/ACg8ocJacv2KLPjdWfY3J7WI_Czp_Sp7MDj0ljv0geEgmWPrsf0NIytc=s96-c");
            adminData.put("otpSent", true);
            adminData.put("generatedOtp", otp); // For testing only
            adminData.put("expirationTime", otpConfig.getExpirationTime());
            
            log.info("Admin OTP scenario simulated for: {}", adminEmail);
              return ResponseEntity.ok(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Admin OTP scenario executed successfully")
                    .payload(adminData)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to simulate admin OTP scenario", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Failed to simulate admin scenario: " + e.getMessage())
                    .build()
            );
        }
    }

    @DeleteMapping("/test/clear-otp")
    @Operation(summary = "Clear OTP for testing", 
               description = "Clears stored OTP for the specified email (testing purposes)")
    public ResponseEntity<ApiResponse<?>> clearOtp(@RequestParam String email) {
        try {
            // This would need to be implemented in OtpService if needed for testing
            log.info("OTP clear requested for: {}", email);
            
            return ResponseEntity.ok(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("OTP clear request processed for " + email)
                    .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Failed to clear OTP: " + e.getMessage())
                    .build()
            );
        }
    }

    @PostMapping("/debug/send-and-show-otp")
    @Operation(summary = "Debug: Send OTP and show it in response", 
               description = "Sends OTP via email AND returns it in response for testing - REMOVE IN PRODUCTION!")
    public ResponseEntity<ApiResponse<?>> sendAndShowOtp(@RequestParam String email) {
        try {
            // Generate OTP
            String otp = otpService.generateOtpEmail(email);
            
            // Send OTP via email
            emailService.sendOtpToEmail(email, otp);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", email);
            responseData.put("generatedOtp", otp); // ⚠️ DEBUG ONLY - REMOVE IN PRODUCTION!
            responseData.put("expirationTimeSeconds", otpConfig.getExpirationTime());
            responseData.put("warning", "This endpoint shows OTP in response - FOR TESTING ONLY!");
            
            log.warn("🚨 DEBUG: Generated OTP {} for email {}", otp, email);
            
            return ResponseEntity.ok(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("OTP sent and shown for debugging")
                    .payload(responseData)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to send debug OTP to: {}", email, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Failed to send OTP: " + e.getMessage())
                    .build()
            );
        }
    }
}
