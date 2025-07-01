package com.example.tb.authentication.service.email;

import jakarta.mail.MessagingException;
import com.example.tb.model.response.EventResponse;
import com.example.tb.model.response.PaymentResponse;

import java.io.UnsupportedEncodingException;

public interface EmailService {
    void sendRegistrationInvitation(String email, String name, String msg) throws MessagingException, UnsupportedEncodingException;
    void sendVerificationEmail(String email,String verificationUrl, String otp) throws MessagingException, UnsupportedEncodingException;

    void sendOtpToEmail(String email, String otp) throws MessagingException, UnsupportedEncodingException;

    void sendPasswordResetEmail(String email, String resetUrl) throws MessagingException, UnsupportedEncodingException;

    void sendQRCodeEmail(String email, String userName, EventResponse event, byte[] qrCodeBytes) throws MessagingException, UnsupportedEncodingException;
    
    void sendCheckInConfirmationEmail(String email, String userName, String eventId) throws MessagingException, UnsupportedEncodingException;
    
    void sendPaymentConfirmationEmail(String email, PaymentResponse payment, EventResponse event) throws MessagingException, UnsupportedEncodingException;
}
