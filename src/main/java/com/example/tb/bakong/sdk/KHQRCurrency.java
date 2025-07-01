package com.example.tb.bakong.sdk;

/**
 * Currency enumeration for KHQR
 * Stub implementation matching official Bakong KHQR SDK interface
 */
public enum KHQRCurrency {
    KHR("116"),  // Cambodian Riel
    USD("840");  // US Dollar
    
    private final String code;
    
    KHQRCurrency(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}
