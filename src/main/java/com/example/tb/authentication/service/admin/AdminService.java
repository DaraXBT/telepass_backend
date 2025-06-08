package com.example.tb.authentication.service.admin;

import com.example.tb.authentication.auth.InfoChangePassword;
import com.example.tb.model.request.AdminRequest;
import jakarta.mail.MessagingException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Service
public interface AdminService extends UserDetailsService {
    void saveUser(AdminRequest adminRequest) throws MessagingException, UnsupportedEncodingException;
    void changePassword(UUID id, InfoChangePassword password);
    void sendOtpViaEmail(String email) throws MessagingException, UnsupportedEncodingException;
    void verifiedEmailByOtp(String email);
    void sendEmail(String email, String name, String msg) throws MessagingException, UnsupportedEncodingException;
    boolean verifyEmailToken(String token);

    String generateGoogleSecret(String username);

    boolean verifyGoogleCode(String username, int code);

}
