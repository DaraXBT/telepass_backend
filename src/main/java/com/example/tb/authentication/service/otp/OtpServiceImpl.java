package com.example.tb.authentication.service.otp;


import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class OtpServiceImpl implements OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);
    private final Map<String, String> otpStore = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    @Override
    public String generateOtp(String username) {
        String otp = String.format("%06d", random.nextInt(1000000));
        otpStore.put(username, otp);
        // Set a TTL for the OTP, e.g., 5 minutes
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        otpStore.remove(username);
                    }
                },
                TimeUnit.MINUTES.toMillis(2)
        );
        return otp;
    }


    @Override
    public String generateOtpEmail(String email) {
        String otp = String.format("%06d", random.nextInt(1000000));
        otpStore.put(email, otp);
        // Set a TTL for the OTP, e.g., 5 minutes
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        otpStore.remove(email);
                    }
                },
                TimeUnit.MINUTES.toMillis(2)
        );
        return otp;
    }    @Override
    public boolean validateOtp(String username, String otp) {
        logger.debug("Validating OTP for username: {}", username);
        return otp.equals(otpStore.get(username));
    }

    @Override
    public boolean validateOtpEmail(String email, String otp) {
        return otp.equals(otpStore.get(email));
    }

    @Override
    public String generateEmailToken(String email) {
        String randomNum = String.format("%06d", random.nextInt(1000000));
        String combined = email + ":" + randomNum;
        String token = Base64.encodeBase64URLSafeString(combined.getBytes(StandardCharsets.UTF_8));
        otpStore.put(token, email);
        // Set a TTL for the token, e.g., 2 minutes
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        otpStore.remove(token);
                    }
                },
                TimeUnit.MINUTES.toMillis(2)
        );
        return token;
    }

    public String decodeEmailToken(String token) {
        byte[] decodedBytes = Base64.decodeBase64(token);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
        return decodedString.split(":")[0];
    }

    public boolean validateEmailToken(String token, String email) {
        String storedEmail = otpStore.get(token);
        return storedEmail != null && storedEmail.equals(email);
    }
}
