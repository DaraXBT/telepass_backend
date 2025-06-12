package com.example.tb.authentication.service.admin;

import com.example.tb.authentication.auth.InfoChangePassword;
import com.example.tb.authentication.repository.admin.AdminRepository;
import com.example.tb.authentication.repository.token.TokenRepository;
import com.example.tb.authentication.service.email.EmailService;
import com.example.tb.authentication.service.otp.OtpService;
import com.example.tb.exception.InternalServerErrorException;
import com.example.tb.exception.NotFoundExceptionClass;
import com.example.tb.model.VerificationToken;
import com.example.tb.model.entity.Admin;
import com.example.tb.model.request.AdminRequest;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AdminServiceImpl implements AdminService {
    private final AdminRepository adminrepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    @Value("${myAdmin.username}")
    private String adminUsername;
    @Value("${myAdmin.password}")
    private String adminPassword;
    @Value("${BaseUrl}")
    private String baseUrl;
    public AdminServiceImpl(AdminRepository adminrepository, PasswordEncoder passwordEncoder, TokenRepository tokenRepository, OtpService otpService, EmailService emailService) {
        this.adminrepository = adminrepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.otpService = otpService;
        this.emailService = emailService;
    }
    @PostConstruct
    public void createAdmin() {
        if (adminrepository.findAll().isEmpty()){
            Admin admin = new Admin();
            admin.setUsername(adminUsername);
            String pass = passwordEncoder.encode(adminPassword);
            admin.setPassword(pass);
            admin.setEmail("daraa.veasna@gmail.com");
            System.out.println("Creating admin..." + admin);
            adminrepository.save(admin);
        }else{
            System.out.println("already have admin");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username){
        UserDetails userDetails = adminrepository.findByUsername(username);
        return userDetails;
    }

    @Override
    public void saveUser(AdminRequest adminRequest) throws MessagingException, UnsupportedEncodingException {
        if (adminRequest == null) {
            throw new IllegalArgumentException("AuthRequest cannot be null");
        } else {
            Boolean username = adminrepository.existsByUsername(adminRequest.getUsername());
            Boolean email = adminrepository.existsByEmail(adminRequest.getEmail());
            if (username) {
                throw new InternalServerErrorException(
                        "Username already exists"
                );
            } else if(email) {
                throw new InternalServerErrorException(
                        "Email already exists"
                );
            } else {
                adminRequest.setPassword(passwordEncoder.encode(adminRequest.getPassword()));
                adminrepository.save(adminRequest.toEntity());
                String verificationToken = UUID.randomUUID().toString();
                saveVerificationToken(adminRequest.getUsername(), verificationToken);

                String otp = otpService.generateOtpEmail(adminRequest.getEmail());
                String verificationUrl = baseUrl + "api/v1/auth/verify-email?token=" + verificationToken;
                emailService.sendVerificationEmail(adminRequest.getEmail(), verificationUrl, otp);
            }
        }
    }

    @Override
    public void changePassword(UUID id, InfoChangePassword password) {
        Optional<Admin> adminOptional = adminrepository.findById(id);
        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            String pass = passwordEncoder.encode(password.getCurrentPassword());
            String newpass = passwordEncoder.encode(password.getNewPassword());

            if (!passwordEncoder.matches(password.getCurrentPassword(), admin.getPassword())) {
                throw new IllegalArgumentException("Current Password isn't correct. Please Try Again.");
            }

            if (passwordEncoder.matches(password.getNewPassword(), admin.getPassword())) {
                throw new IllegalArgumentException("Your new password is still the same as your old password");
            }

            if (!password.getNewPassword().equals(password.getConfirmPassword())) {
                throw new IllegalArgumentException("Your confirm password does not match with your new password");
            }

            admin.setPassword(newpass);
            adminrepository.save(admin);
        } else {
            throw new NotFoundExceptionClass("Admin not found with ID: " + id);
        }
    }
    public void saveVerificationToken(String username, String token) {
        Admin admin = adminrepository.findByUsernameReturnAuth(username);
        VerificationToken verificationToken = new VerificationToken(token, admin);
        tokenRepository.save(verificationToken);
    }

    @Override
    public void sendOtpViaEmail(String email) throws MessagingException, UnsupportedEncodingException {
        Admin admin = adminrepository.findUserByEmail(email);
        if (admin != null) {
            String otp = otpService.generateOtpEmail(email);
            emailService.sendOtpToEmail(email, otp);
        } else {
            throw new NotFoundExceptionClass("User not found with " + email);
        }
    }

    @Override
    public void verifiedEmailByOtp(String email) {
        Admin admin = adminrepository.findUserByEmail(email);
        admin.setEnabled(true);
        adminrepository.save(admin);
    }

    @Override
    public void sendEmail(String email, String name, String msg) throws MessagingException, UnsupportedEncodingException {
        emailService.sendRegistrationInvitation(email, name, msg);
    }

    @Override
    public boolean verifyEmailToken(String token) {
        Optional<VerificationToken> optionalToken = tokenRepository.findByToken(token);
        if (optionalToken.isPresent()) {
            VerificationToken verificationToken = optionalToken.get();
            Admin admin = verificationToken.getAdmin();
            admin.setEnabled(true);
            adminrepository.save(admin);
            tokenRepository.delete(verificationToken);
            return true;        }
        return false;
    }
    
    @Override
    public Admin findByGoogleIdOrEmail(String googleId, String email) {
        return adminrepository.findByGoogleIdOrEmail(googleId, email);
    }    @Override
    public Admin registerGoogleAdmin(AdminRequest adminRequest) throws MessagingException, UnsupportedEncodingException {
        if (adminRequest == null) {
            throw new IllegalArgumentException("AdminRequest cannot be null");
        }
        
        log.info("Attempting to register Google admin with email: {} and googleId: {}", 
                adminRequest.getEmail(), adminRequest.getGoogleId());
        
        // Check if admin already exists with this Google ID or email
        Admin existingAdmin = adminrepository.findByGoogleIdOrEmail(
            adminRequest.getGoogleId(), adminRequest.getEmail());
        
        if (existingAdmin != null) {
            log.info("Found existing admin with ID: {}", existingAdmin.getId());
            
            // Always enable Google accounts and update info
            existingAdmin.setEnabled(true);
            
            // Update existing admin with Google info if needed
            if (existingAdmin.getGoogleId() == null && adminRequest.getGoogleId() != null) {
                existingAdmin.setGoogleId(adminRequest.getGoogleId());
                existingAdmin.setGoogleAccount(true);
                existingAdmin.setFullName(adminRequest.getFullName());
                existingAdmin.setProfileImage(adminRequest.getProfileImage());
                log.info("Updating existing admin with Google info");
            } else {
                log.info("Admin already has Google info, returning existing admin");
            }
            
            Admin savedAdmin = adminrepository.save(existingAdmin);
            log.info("Updated/returned existing admin with ID: {}", savedAdmin.getId());
            return savedAdmin;
        }        // Create new admin with Google info
        log.info("Creating new Google admin for email: {}", adminRequest.getEmail());
        
        // Check if username or email already exists (but without Google ID)
        Boolean usernameExists = adminrepository.existsByUsername(adminRequest.getUsername());
        Boolean emailExists = adminrepository.existsByEmail(adminRequest.getEmail());
        
        if (usernameExists) {
            // If username exists, generate a unique one
            String originalUsername = adminRequest.getUsername();
            String newUsername = originalUsername + "_google_" + System.currentTimeMillis();
            adminRequest.setUsername(newUsername);
            log.info("Username {} already exists, using: {}", originalUsername, newUsername);
        }
        
        if (emailExists) {
            // This shouldn't happen as we checked above, but just in case
            log.warn("Email {} already exists in system but not linked to Google ID", adminRequest.getEmail());
        }
        
        // Validate required fields
        if (adminRequest.getUsername() == null || adminRequest.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        if (adminRequest.getEmail() == null || adminRequest.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        // For Google accounts, set a default encoded password if none provided
        if (adminRequest.getPassword() == null || adminRequest.getPassword().trim().isEmpty()) {
            adminRequest.setPassword(passwordEncoder.encode("GOOGLE_AUTH_" + UUID.randomUUID().toString()));
        } else {
            adminRequest.setPassword(passwordEncoder.encode(adminRequest.getPassword()));
        }
        adminRequest.setEnabled(true); // Google accounts are enabled by default
        adminRequest.setGoogleAccount(true);
        
        Admin newAdmin = adminrepository.save(adminRequest.toEntity());
        log.info("Successfully created new Google admin with ID: {}", newAdmin.getId());
        return newAdmin;
    }
}
