package com.example.tb.authentication.service.otp;

public interface OtpService {

    String generateOtp(String username);

    String generateOtpEmail(String email);

    boolean validateOtp(String username, String otp);
    boolean validateOtpEmail(String email, String otp);

    String generateEmailToken(String email);

    String decodeEmailToken(String token);

    boolean validateEmailToken(String token, String email);

    // DEBUG METHOD - REMOVE IN PRODUCTION
    String getCurrentOtpForDebug(String email);
}
