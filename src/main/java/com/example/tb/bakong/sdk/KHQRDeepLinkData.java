package com.example.tb.bakong.sdk;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * KHQRDeepLinkData class for Bakong KHQR SDK Deep Link response
 * Based on official NBC Bakong KHQR SDK documentation
 * 
 * This is a stub implementation matching the official API structure.
 * Replace with actual SDK when available in Maven repositories.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KHQRDeepLinkData {
    
    private String shortLink;      // Generated short link (e.g., https://bakongsit.page.link/xxx)
    private String originalQr;     // Original QR code string
    private String expiresAt;      // Expiration timestamp
    private String generatedAt;    // Generation timestamp
    
    // Constructor with short link only
    public KHQRDeepLinkData(String shortLink) {
        this.shortLink = shortLink;
        this.generatedAt = String.valueOf(System.currentTimeMillis());
    }
    
    // Validation method
    public boolean isValid() {
        return shortLink != null && !shortLink.trim().isEmpty();
    }
}
