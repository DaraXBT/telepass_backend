package com.example.tb.bakong.sdk;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * KHQRDecodeData class for Bakong KHQR SDK
 * Based on official NBC Bakong KHQR SDK documentation decode result structure
 * 
 * This is a stub implementation matching the official API structure.
 * Replace with actual SDK when available in Maven repositories.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KHQRDecodeData {
    
    // EMV Standard Fields
    private String payloadFormatIndicator;      // "01" - EMV format version
    private String pointOfInitiationMethod;     // "12" - Dynamic QR
    private String merchantType;                // "29" for Individual, "30" for Merchant
    
    // Bakong Account Information
    private String bakongAccountID;             // Bakong account identifier
    private String merchantId;                  // Merchant ID (for merchant QR)
    private String accountInformation;          // Additional account info
    private String upiAccountInformation;       // UPI account info
    private String acquiringBank;               // Bank name
    
    // EMV Transaction Fields
    private String merchantCategoryCode;        // "5999" - General services
    private String countryCode;                 // "KH" - Cambodia
    private String merchantName;                // Merchant/Individual name
    private String merchantCity;                // City name
    private String transactionCurrency;        // "840" for USD, "116" for KHR
    private String transactionAmount;           // Amount as string
    
    // Optional Transaction Fields
    private String billNumber;                  // Invoice/bill reference
    private String mobileNumber;                // Contact number
    private String storeLabel;                  // Store identifier
    private String terminalLabel;               // Terminal identifier
    private String purposeOfTransaction;        // Transaction description
    
    // Khmer Language Support
    private String merchantAlternateLanguagePreference; // "km" for Khmer
    private String merchantNameAlternateLanguage;       // Khmer merchant name
    private String merchantCityAlternateLanguage;       // Khmer city name
    
    // Technical Fields
    private String timestamp;                   // QR generation timestamp
    private String crc;                         // CRC checksum
    
    // Helper methods for data conversion
    public Double getAmount() {
        try {
            return transactionAmount != null ? Double.parseDouble(transactionAmount) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    // Currency code conversion
    public String getCurrency() {
        if ("840".equals(transactionCurrency)) return "USD";
        if ("116".equals(transactionCurrency)) return "KHR";
        return transactionCurrency;
    }
    
    // Get currency as enum
    public KHQRCurrency getCurrencyEnum() {
        if ("840".equals(transactionCurrency)) return KHQRCurrency.USD;
        if ("116".equals(transactionCurrency)) return KHQRCurrency.KHR;
        return KHQRCurrency.KHR; // Default
    }
    
    // Check if this is a merchant QR
    public boolean isMerchantQR() {
        return "30".equals(merchantType);
    }
    
    // Check if this is an individual QR
    public boolean isIndividualQR() {
        return "29".equals(merchantType);
    }
    
    // Validation methods
    public boolean isValid() {
        return payloadFormatIndicator != null && 
               pointOfInitiationMethod != null &&
               merchantType != null &&
               bakongAccountID != null &&
               crc != null;
    }
    
    @Override
    public String toString() {
        return String.format("KHQRDecodeData{merchantType='%s', bakongAccountID='%s', " +
                           "merchantId='%s', merchantName='%s', amount='%s', currency='%s', " +
                           "acquiringBank='%s', billNumber='%s', timestamp='%s', crc='%s'}",
                           merchantType, bakongAccountID, merchantId, merchantName, 
                           transactionAmount, getCurrency(), acquiringBank, billNumber, timestamp, crc);
    }
}
