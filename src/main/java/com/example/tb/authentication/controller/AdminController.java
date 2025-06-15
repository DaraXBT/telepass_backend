package com.example.tb.authentication.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.tb.authentication.repository.token.TokenRepository;
import com.example.tb.model.VerificationToken;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tb.authentication.auth.AuthenticationRequest;
import com.example.tb.authentication.auth.InfoChangePassword;
import com.example.tb.authentication.repository.admin.AdminRepository;
import com.example.tb.authentication.service.admin.AdminService;
import com.example.tb.authentication.service.otp.OtpService;
import com.example.tb.jwt.JwtResponse;
import com.example.tb.jwt.JwtTokenUtils;
import com.example.tb.model.entity.Admin;
import com.example.tb.model.request.AdminRequest;
import com.example.tb.model.request.OtpRequest;
import com.example.tb.model.request.OtpRequestEmail;
import com.example.tb.model.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin
@Slf4j
public class AdminController {
    private final AdminService adminService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtils jwtTokenUtils;
    private final OtpService otpService;
    private final AdminRepository adminRepository;
    private final TokenRepository tokenRepository;
    @Value("${WebBaseUrl}")
    private String webBaseUrl;

    public AdminController(AdminService adminService, AuthenticationManager authenticationManager,
                           JwtTokenUtils jwtTokenUtils, OtpService otpService, AdminRepository adminRepository, TokenRepository tokenRepository) {
        this.adminService = adminService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtils = jwtTokenUtils;
        this.otpService = otpService;
        this.adminRepository = adminRepository;
        this.tokenRepository = tokenRepository;
    }    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest jwtRequest) {
        try {
            // First check if admin exists and is enabled
            Admin admin = adminRepository.findByUsernameReturnAuth(jwtRequest.getUsername());
            if (admin != null && !admin.isEnabled()) {
                throw new IllegalArgumentException("Account not yet verified. Please check your email for verification instructions.");
            }
            
            authenticate(jwtRequest.getUsername(), jwtRequest.getPassword());
        } catch (Exception e) {
            log.error("Login failed for username: {}, reason: {}", jwtRequest.getUsername(), e.getMessage());
            throw new IllegalArgumentException(e.getMessage().contains("Account not yet verified") ? 
                e.getMessage() : "Invalid username or password");
        }

        final UserDetails userDetails = adminService.loadUserByUsername(jwtRequest.getUsername());
        final String token = jwtTokenUtils.generateToken(userDetails);
        return ResponseEntity.ok(new JwtResponse(LocalDateTime.now(), jwtRequest.getUsername(), token));
    }    private void authenticate(String username, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new Exception("Account not yet verified. Please check your email for verification instructions.", e);
        } catch (BadCredentialsException e) {
            throw new Exception("Invalid username or password", e);
        }
    }

    @PutMapping("/change-password")
    @Operation(summary = "change password")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<?> changePassword(@Valid @RequestBody InfoChangePassword changePassword) {
        Admin currentUser = (Admin) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID id = currentUser.getId();
        adminService.changePassword(id, changePassword);
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("successfully change password")
                .build();
    }    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody AdminRequest adminRequest)
            throws MessagingException, UnsupportedEncodingException {
        
        // Add debug logging to see what we're receiving
        log.info("Received registration request - isGoogleAccount: {}, googleId: {}, email: {}", 
                adminRequest.isGoogleAccount(), adminRequest.getGoogleId(), adminRequest.getEmail());
        log.debug("Full AdminRequest: {}", adminRequest);
        
        // Check if this is a Google account registration
        if (adminRequest.isGoogleAccount() && adminRequest.getGoogleId() != null) {
            log.info("Received Google admin registration request for email: {} with googleId: {}", 
                    adminRequest.getEmail(), adminRequest.getGoogleId());
            
            try {
                Admin registeredAdmin = adminService.registerGoogleAdmin(adminRequest);
                log.info("Successfully registered/found admin with ID: {} and username: {}", 
                        registeredAdmin.getId(), registeredAdmin.getUsername());
                
                // Generate JWT token for the new admin
                final UserDetails userDetails = adminService.loadUserByUsername(registeredAdmin.getUsername());
                final String token = jwtTokenUtils.generateToken(userDetails);
                log.info("Generated JWT token successfully for registered admin: {}", registeredAdmin.getUsername());
                
                // Create response with admin data and token
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("username", registeredAdmin.getUsername());
                responseData.put("email", registeredAdmin.getEmail());
                responseData.put("fullName", registeredAdmin.getFullName());
                responseData.put("profileImage", registeredAdmin.getProfileImage());
                responseData.put("googleId", registeredAdmin.getGoogleId());
                responseData.put("isGoogleAccount", registeredAdmin.isGoogleAccount());                responseData.put("token", token);
                responseData.put("accessToken", token);
                
                log.info("Returning success response for Google admin registration: {}", registeredAdmin.getUsername());
                return ApiResponse.builder()
                        .payload(responseData)
                        .message("Google admin registered successfully")
                        .date(LocalDateTime.now())
                        .build();
            } catch (Exception e) {
                log.error("Failed to register Google admin: {}", e.getMessage(), e);
                return ApiResponse.builder()
                        .message("google-auth-error")
                        .date(LocalDateTime.now())
                        .build();
            }
        } else {
            log.info("Taking regular registration path - isGoogleAccount: {}, googleId: {}", 
                    adminRequest.isGoogleAccount(), adminRequest.getGoogleId());
            // Regular admin registration
            adminService.saveUser(adminRequest);
            return ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Register successfully")
                    .build();        }
    }    @PostMapping("/check-google-account")
    @Operation(summary = "Check if admin exists with Google credentials")
    public ResponseEntity<ApiResponse<?>> checkGoogleAccount(@RequestBody Map<String, String> request) {
        log.info("Received Google account check request: {}", request);
        String googleId = request.get("googleId");
        String email = request.get("email");
        log.info("Extracted googleId: {}, email: {}", googleId, email);
        
        if ((googleId == null || googleId.isEmpty()) && (email == null || email.isEmpty())) {
            log.warn("Missing required Google identification data");
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .message("Missing required Google identification data")
                    .date(LocalDateTime.now())
                    .build());
        }        
        try {
            log.info("Looking for admin with googleId: {} or email: {}", googleId, email);
            Admin admin = adminService.findByGoogleIdOrEmail(googleId, email);
            if (admin != null) {
                log.info("Found admin: {}, generating JWT token", admin.getUsername());
                // Generate JWT token for the admin
                final UserDetails userDetails = adminService.loadUserByUsername(admin.getUsername());
                final String token = jwtTokenUtils.generateToken(userDetails);
                log.info("Generated JWT token successfully for admin: {}", admin.getUsername());
                
                // Create response with admin data and token
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("username", admin.getUsername());
                responseData.put("email", admin.getEmail());
                responseData.put("fullName", admin.getFullName());
                responseData.put("profileImage", admin.getProfileImage());
                responseData.put("googleId", admin.getGoogleId());
                responseData.put("isGoogleAccount", admin.isGoogleAccount());                responseData.put("token", token);
                responseData.put("accessToken", token);
                
                log.info("Returning success response for admin: {}", admin.getUsername());
                return ResponseEntity.ok(ApiResponse.builder()
                        .payload(responseData)
                        .message("Admin found with Google credentials")
                        .date(LocalDateTime.now())
                        .build());
            } else {
                log.warn("No admin found with googleId: {} or email: {}", googleId, email);
                return ResponseEntity.status(404).body(ApiResponse.builder()
                        .message("Admin not found with Google credentials")
                        .date(LocalDateTime.now())
                        .build());
            }
        } catch (Exception e) {
            log.error("Error checking Google account: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.builder()
                    .message("Error checking Google account: " + e.getMessage())
                    .date(LocalDateTime.now())
                    .build());
        }
    }

    @PostMapping("/send-otp-email")
    public ApiResponse<?> sendOtpViaEmail(@RequestParam String email)
            throws MessagingException, UnsupportedEncodingException {
        adminService.sendOtpViaEmail(email);
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("OTP via email sent successfully\"")
                .build();
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<?> validateOtp(@RequestBody OtpRequest otpRequest) {
        boolean isValid = otpService.validateOtp(otpRequest.getUsername(), otpRequest.getOtp());
        if (isValid) {
            // return ResponseEntity.ok("OTP validated successfully.");
            final UserDetails userDetails = adminService.loadUserByUsername(otpRequest.getUsername());
            final String token = jwtTokenUtils.generateToken(userDetails);
            return ResponseEntity.ok(new JwtResponse(LocalDateTime.now(), otpRequest.getUsername(), token));
        } else {
            throw new IllegalArgumentException("Invalid OTP code");
        }
    }

    @PostMapping("/validate-otp-email")
    public ResponseEntity<?> validateOtpEmail(@RequestBody OtpRequestEmail otpRequestEmail) {
        boolean isValid = otpService.validateOtpEmail(otpRequestEmail.getEmail(), otpRequestEmail.getOtp());
        if (isValid) {
            Admin admin = adminRepository.findUserByEmail(otpRequestEmail.getEmail());
            if (admin != null) {
                final UserDetails userDetails = adminService.loadUserByUsername(admin.getUsername());
                final String token = jwtTokenUtils.generateToken(userDetails);
                return ResponseEntity.ok(new JwtResponse(LocalDateTime.now(), admin.getUsername(), token));
            } else {
                throw new IllegalArgumentException("User not found with given email");
            }

        } else {
            throw new IllegalArgumentException("Invalid OTP code");
        }
    }

    @PostMapping("/verified-otp")
    public void verifiedEmailByOtp(@RequestBody OtpRequestEmail otpRequestEmail) {
        boolean isValid = otpService.validateOtpEmail(otpRequestEmail.getEmail(), otpRequestEmail.getOtp());
        if (isValid) {
            Admin auth = adminRepository.findUserByEmail(otpRequestEmail.getEmail());
            if (auth != null) {
                adminService.verifiedEmailByOtp(auth.getEmail());
            } else {
                throw new IllegalArgumentException("User not found with given email");
            }

        } else {
            throw new IllegalArgumentException("Invalid OTP code");
        }
    }

    @PostMapping("/send-email")
    public ApiResponse<?> sendEmail(@RequestParam String email, @RequestParam String name, @RequestParam String msg)
            throws MessagingException, UnsupportedEncodingException {
        adminService.sendEmail(email, name, msg);
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("Email sent successfully")
                .build();
    }

    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        boolean isVerified = adminService.verifyEmailToken(token);
        if (isVerified) {
            response.sendRedirect(webBaseUrl + "email-verified");
        } else {
            response.sendRedirect(webBaseUrl + "invalid-token");
        }
    }

    @GetMapping("/by-email")
    @Operation(summary = "Get admin by email")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Admin> getAdminByEmail(@RequestParam String email) {
        Admin admin = adminRepository.findUserByEmail(email);
        if (admin != null) {
            return ResponseEntity.ok(admin);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/by-id")
    @Operation(summary = "Get admin by ID")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Admin> getAdminById(@RequestParam UUID id) {
        log.info("Fetching admin with ID: {}", id);
        Optional<Admin> adminOptional = adminRepository.findById(id);
        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            log.info("Found admin: {} with email: {}", admin.getUsername(), admin.getEmail());
            return ResponseEntity.ok(admin);
        } else {
            log.warn("Admin not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get admin by ID (RESTful)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Admin> getAdminByIdPath(@PathVariable UUID id) {
        log.info("Fetching admin with ID: {}", id);
        Optional<Admin> adminOptional = adminRepository.findById(id);
        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            log.info("Found admin: {} with email: {}", admin.getUsername(), admin.getEmail());
            return ResponseEntity.ok(admin);
        } else {
            log.warn("Admin not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-username")
    @Operation(summary = "Get admin by username")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Admin> getAdminByUsername(@RequestParam String username) {
        log.info("Fetching admin with username: {}", username);
        Admin admin = adminRepository.findByUsernameReturnAuth(username);
        if (admin != null) {
            log.info("Found admin: {} with email: {}", admin.getUsername(), admin.getEmail());
            return ResponseEntity.ok(admin);
        } else {
            log.warn("Admin not found with username: {}", username);
            return ResponseEntity.notFound().build();
        }
    }

    // Password Reset Endpoints
    @PostMapping("/request-password-reset")
    public ApiResponse<?> requestPasswordReset(@RequestParam String email)
            throws MessagingException, UnsupportedEncodingException {
        log.info("Password reset requested for email: {}", email);
        adminService.sendOtpViaEmail(email);
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("Password reset OTP sent successfully")
                .build();
    }

    @PostMapping("/verify-password-reset-otp")
    public ApiResponse<?> verifyPasswordResetOtp(@RequestBody OtpRequestEmail otpRequest) {
        log.info("Verifying password reset OTP for email: {}", otpRequest.getEmail());
        boolean isValid = otpService.validateOtpEmail(otpRequest.getEmail(), otpRequest.getOtp());
        if (isValid) {
            return ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("OTP verified successfully")
                    .build();
        } else {
            throw new IllegalArgumentException("Invalid OTP code");
        }
    }

    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        String otp = request.get("otp");
        
        log.info("Password reset attempt for email: {}", email);
        
        // Verify OTP first
        boolean isValid = otpService.validateOtpEmail(email, otp);
        if (!isValid) {
            throw new IllegalArgumentException("Invalid or expired OTP code");
        }
        
        // Find admin and update password
        Admin admin = adminRepository.findUserByEmail(email);
        if (admin == null) {
            throw new IllegalArgumentException("User not found with given email");
        }
        
        // Update password using admin service (this will handle encoding)
        adminService.resetPassword(email, newPassword);
        
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("Password reset successfully")
                .build();
    }

    @PostMapping("/validate-otp-to-verified-register")
    public ApiResponse<?> validateOtpToVerifiedRegister(@RequestBody OtpRequestEmail otpRequest) {
        log.info("Validating OTP for registration verification: {}", otpRequest.getEmail());
        boolean isValid = otpService.validateOtpEmail(otpRequest.getEmail(), otpRequest.getOtp());
        if (isValid) {
            adminService.verifiedEmailByOtp(otpRequest.getEmail());
            return ApiResponse.builder()
                    .date(LocalDateTime.now())
                    .message("Account verified successfully")
                    .build();
        } else {
            throw new IllegalArgumentException("Invalid OTP code");
        }
    }

    @PostMapping("/check-verification-status")
    @Operation(summary = "Check admin verification status")
    public ResponseEntity<ApiResponse<?>> checkVerificationStatus(@RequestParam String email) {
        log.info("Checking verification status for email: {}", email);
        Admin admin = adminRepository.findUserByEmail(email);
        if (admin != null) {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", email);
            responseData.put("username", admin.getUsername());
            responseData.put("isVerified", admin.isEnabled());
            responseData.put("isGoogleAccount", admin.isGoogleAccount());
            
            String message = admin.isEnabled() ? 
                "Account is verified and can login" : 
                "Account exists but not yet verified. Please check your email for verification instructions.";
            
            return ResponseEntity.ok(ApiResponse.builder()
                    .payload(responseData)
                    .message(message)
                    .date(LocalDateTime.now())
                    .build());
        } else {
            return ResponseEntity.status(404).body(ApiResponse.builder()
                    .message("No account found with this email address")
                    .date(LocalDateTime.now())
                    .build());
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset email")
    public ApiResponse<?> forgotPassword(@RequestParam String email)
            throws MessagingException, UnsupportedEncodingException {
        log.info("Forgot password request for email: {}", email);
        adminService.sendPasswordResetEmail(email);
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("Password reset link sent to your email successfully")
                .build();
    }

    @GetMapping("/verify-email-forget-password")
    @Operation(summary = "Verify password reset token and redirect to change password page")
    public void verifyPasswordResetToken(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        log.info("Verifying password reset token: {}", token);
        boolean isVerified = adminService.verifyPasswordResetToken(token);
        if (isVerified) {
            log.info("Password reset token verified, redirecting to change password page");
            response.sendRedirect(webBaseUrl + "reset-password?token=" + token);
        } else {
            log.warn("Invalid password reset token, redirecting to invalid token page");
            response.sendRedirect(webBaseUrl + "invalid-token");
        }
    }

    @PostMapping("/change-password-with-token")
    @Operation(summary = "Change password using reset token")
    @Transactional
    public ApiResponse<?> changePasswordWithToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        log.info("Password change attempt with token");
        
        // Verify token first
        boolean isValid = adminService.verifyPasswordResetToken(token);
        if (!isValid) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        
        // Find admin associated with this token
        Optional<VerificationToken> optionalToken = tokenRepository.findByToken(token);
        if (!optionalToken.isPresent()) {
            throw new IllegalArgumentException("Invalid reset token");
        }
        
        VerificationToken verificationToken = optionalToken.get();
        Admin admin = verificationToken.getAdmin();
        
        // Update password using admin service (this will handle encoding)
        adminService.resetPassword(admin.getEmail(), newPassword);
        
        // Delete the verification token after successful password change
        tokenRepository.delete(verificationToken);
        
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("Password changed successfully")
                .build();
    }

    // DEBUG ENDPOINT - REMOVE IN PRODUCTION
    @GetMapping("/debug/show-otp")
    public ResponseEntity<Map<String, Object>> showCurrentOtp(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            // This is a debug method - we'll need to add this to OtpService
            String currentOtp = otpService.getCurrentOtpForDebug(email);
            response.put("email", email);
            response.put("currentOtp", currentOtp);
            response.put("warning", "DEBUG ONLY - REMOVE IN PRODUCTION");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
