package com.example.tb.authentication.service.payment;

import com.example.tb.configuration.BakongConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Bakong KHQR signature verification utility
 * Implements KHQR standard signature verification for webhook callbacks and API requests
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BakongSignatureUtil {
    
    private final BakongConfig bakongConfig;
    
    /**
     * Verify webhook signature according to KHQR standards
     * 
     * @param payload Raw payload string
     * @param signature Signature from Bakong
     * @param timestamp Request timestamp
     * @return true if signature is valid
     */
    public boolean verifyWebhookSignature(String payload, String signature, String timestamp) {
        try {
            String expectedSignature = generateWebhookSignature(payload, timestamp);
            return secureCompare(signature, expectedSignature);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }
    
    /**
     * Generate signature for API requests
     * 
     * @param method HTTP method
     * @param uri Request URI
     * @param params Request parameters
     * @param timestamp Request timestamp
     * @return Generated signature
     */
    public String generateApiSignature(String method, String uri, Map<String, Object> params, String timestamp) {
        try {
            String stringToSign = buildStringToSign(method, uri, params, timestamp);
            return hmacSha256(stringToSign, bakongConfig.getApi().getSecretKey());
        } catch (Exception e) {
            log.error("Error generating API signature", e);
            throw new RuntimeException("Failed to generate API signature", e);
        }
    }
    
    /**
     * Generate webhook signature
     * 
     * @param payload Request payload
     * @param timestamp Request timestamp
     * @return Generated signature
     */
    private String generateWebhookSignature(String payload, String timestamp) throws Exception {
        String stringToSign = timestamp + "." + payload;
        return hmacSha256(stringToSign, bakongConfig.getApi().getSecretKey());
    }
    
    /**
     * Build string to sign for API requests according to KHQR standards
     * 
     * @param method HTTP method
     * @param uri Request URI
     * @param params Request parameters
     * @param timestamp Request timestamp
     * @return String to sign
     */
    private String buildStringToSign(String method, String uri, Map<String, Object> params, String timestamp) {
        // Sort parameters alphabetically (KHQR requirement)
        TreeMap<String, Object> sortedParams = new TreeMap<>(params);
        
        String queryString = sortedParams.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        
        return method.toUpperCase() + "\n" +
               uri + "\n" +
               queryString + "\n" +
               timestamp;
    }
    
    /**
     * Generate HMAC-SHA256 signature
     * 
     * @param data Data to sign
     * @param key Secret key
     * @return Base64 encoded signature
     */
    private String hmacSha256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
    
    /**
     * Secure string comparison to prevent timing attacks
     * 
     * @param a First string
     * @param b Second string
     * @return true if strings are equal
     */
    private boolean secureCompare(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    /**
     * Validate timestamp to prevent replay attacks
     * 
     * @param timestamp Request timestamp
     * @param toleranceSeconds Allowed time difference in seconds
     * @return true if timestamp is valid
     */
    public boolean isValidTimestamp(String timestamp, long toleranceSeconds) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            long timeDiff = Math.abs(currentTime - requestTime);
            
            return timeDiff <= toleranceSeconds;
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }
}
