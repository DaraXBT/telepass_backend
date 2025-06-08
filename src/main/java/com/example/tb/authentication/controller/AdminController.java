package com.example.tb.authentication.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.UUID;

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
import com.example.tb.model.response.ApiResponse;
import com.example.tb.jwt.JwtResponse;
import com.example.tb.jwt.JwtTokenUtils;
import com.example.tb.model.entity.Admin;
import com.example.tb.model.request.AdminRequest;
import com.example.tb.model.request.OtpRequest;
import com.example.tb.model.request.OtpRequestEmail;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin
public class AdminController {
    private final AdminService adminService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtils jwtTokenUtils;
    private final OtpService otpService;
    private final AdminRepository adminRepository;
    @Value("${WebBaseUrl}")
    private String webBaseUrl;

    public AdminController(AdminService adminService, AuthenticationManager authenticationManager,
            JwtTokenUtils jwtTokenUtils, OtpService otpService, AdminRepository adminRepository) {
        this.adminService = adminService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtils = jwtTokenUtils;
        this.otpService = otpService;
        this.adminRepository = adminRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest jwtRequest) {
        try {
            authenticate(jwtRequest.getUsername(), jwtRequest.getPassword());// emailService.sendEmail("sovitasovita28@gmail.com",
                                                                             // "Digital TTEST", "Welcome test");
        } catch (Exception e) {
            // e.printStackTrace();
            throw new IllegalArgumentException("Invalid username or password");
        }

        final UserDetails userDetails = adminService.loadUserByUsername(jwtRequest.getUsername());
        final String token = jwtTokenUtils.generateToken(userDetails);
        return ResponseEntity.ok(new JwtResponse(LocalDateTime.now(), jwtRequest.getUsername(), token));
    }

    private void authenticate(String username, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
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
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody AdminRequest adminRequest)
            throws MessagingException, UnsupportedEncodingException {
        adminService.saveUser(adminRequest);
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("Register successfully")
                .build();
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

    @PostMapping("/ga-secret")
    public ApiResponse<?> generateGoogleSecret(@RequestParam String username) {
        String secret = adminService.generateGoogleSecret(username);
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message("Google Authenticator secret generated")
                .payload(secret)
                .build();
    }

    @PostMapping("/ga-verify")
    public ApiResponse<?> verifyGoogleCode(@RequestParam String username, @RequestParam int code) {
        boolean valid = adminService.verifyGoogleCode(username, code);
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .message(valid ? "Code verified" : "Invalid code")
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
}
