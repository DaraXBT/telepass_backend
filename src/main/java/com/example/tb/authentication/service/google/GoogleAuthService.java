package com.example.tb.authentication.service.google;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }
}
