package com.example.tb.authentication.service.email;


import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
      public EmailServiceImpl() {
        log.info("EmailServiceImpl initialized in no-op mode - emails will be logged but not sent");
    }

    @Override
    public void sendRegistrationInvitation(String email, String name, String msg) throws MessagingException, UnsupportedEncodingException {
        log.info("NO-OP: Would send registration invitation to email: {}, name: {}, message: {}", email, name, msg);
        // Email sending disabled - this is a no-op implementation
    }

    @Override
    public void sendVerificationEmail(String email, String verificationUrl, String otp) throws MessagingException, UnsupportedEncodingException {
        log.info("NO-OP: Would send verification email to: {}, URL: {}, OTP: {}", email, verificationUrl, otp);
        // Email sending disabled - this is a no-op implementation
    }

    @Override
    public void sendOtpToEmail(String email, String otp) throws MessagingException, UnsupportedEncodingException {
        log.info("NO-OP: Would send OTP to email: {}, OTP: {}", email, otp);
        // Email sending disabled - this is a no-op implementation
    }
}
