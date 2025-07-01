package com.example.tb.bakong.sdk;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * SourceInfo class for Bakong KHQR SDK Deep Link generation
 * Based on official NBC Bakong KHQR SDK documentation
 * 
 * This is a stub implementation matching the official API structure.
 * Replace with actual SDK when available in Maven repositories.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceInfo {
    
    private String appIconUrl;           // URL to app icon for deep link display
    private String appName;              // Application name
    private String appDeepLinkCallback;  // Callback URL for app opening after payment
    private String appScheme;            // Custom app scheme (e.g., "bakong", "telepass")
    private String callbackUrl;          // Alternative callback URL field
    
    // Constructor matching official SDK documentation
    public SourceInfo(String appIconUrl, String appName, String appDeepLinkCallback) {
        this.appIconUrl = appIconUrl;
        this.appName = appName;
        this.appDeepLinkCallback = appDeepLinkCallback;
        this.callbackUrl = appDeepLinkCallback; // Map to callback URL
        this.appScheme = "bakong"; // Default scheme
    }
    
    // Validation method
    public boolean isValid() {
        return appName != null && !appName.trim().isEmpty() &&
               appIconUrl != null && !appIconUrl.trim().isEmpty();
    }
}
