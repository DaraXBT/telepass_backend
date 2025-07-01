package com.example.tb.bakong.sdk;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * IndividualInfo class for Bakong KHQR SDK
 * Based on official NBC Bakong KHQR SDK documentation
 * 
 * This is a stub implementation matching the official API structure.
 * Replace with actual SDK when available in Maven repositories.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndividualInfo {
    
    // Required fields for individual KHQR generation
    private String bakongAccountId;          // max 32 chars - Account number or phone number
    private String merchantName;             // max 25 chars - Individual/person name
    
    // Payment details
    private KHQRCurrency currency;           // USD or KHR (default: KHR)
    private Double amount;                   // Transaction amount
    private String merchantCity;             // max 15 chars - Default "Phnom Penh"
    
    // Optional fields
    private String accountInformation;       // max 32 chars - Additional account info
    private String acquiringBank;            // max 32 chars - Bank name (optional for individuals)
    
    // Optional transaction fields
    private String billNumber;               // max 25 chars - Invoice/bill reference
    private String mobileNumber;             // max 25 chars - Contact number
    private String storeLabel;               // max 25 chars - Store identifier
    private String terminalLabel;            // max 25 chars - Terminal identifier
    private String purposeOfTransaction;     // max 25 chars - Transaction description
    private String upiAccountInformation;    // max 31 chars - Extended account info
    
    // Advanced features
    private String merchantAlternateLanguagePreference; // "km" for Khmer
    private String merchantNameAlternateLanguage;       // Khmer name
    private String merchantCityAlternateLanguage;       // Khmer city name
    
    // Additional optional fields
    private String merchantCategoryCode;     // Default "5999" - General services
    private String countryCode;              // Default "KH" - Cambodia
    
    // Constructor with required fields only
    public IndividualInfo(String bakongAccountId, String merchantName, 
                         KHQRCurrency currency, Double amount) {
        this.bakongAccountId = bakongAccountId;
        this.merchantName = merchantName;
        this.currency = currency != null ? currency : KHQRCurrency.KHR;
        this.amount = amount;
        this.merchantCity = "PHNOMPENH"; // Default as per documentation
        this.merchantCategoryCode = "5999"; // General services
        this.countryCode = "KH"; // Cambodia
    }
    
    // Constructor matching official SDK documentation example
    public IndividualInfo(String bakongAccountId, String merchantName, String accountInformation,
                         String acquiringBank, KHQRCurrency currency, Double amount) {
        this.bakongAccountId = bakongAccountId;
        this.merchantName = merchantName;
        this.accountInformation = accountInformation;
        this.acquiringBank = acquiringBank;
        this.currency = currency != null ? currency : KHQRCurrency.KHR;
        this.amount = amount;
        this.merchantCity = "PHNOMPENH"; // Default as per documentation
        this.merchantCategoryCode = "5999"; // General services
        this.countryCode = "KH"; // Cambodia
    }
    
    // Validation methods (as per SDK documentation)
    public boolean isValid() {
        return bakongAccountId != null && !bakongAccountId.trim().isEmpty() &&
               merchantName != null && !merchantName.trim().isEmpty() &&
               currency != null;
    }
    
    // Field length validation
    public boolean validateFieldLengths() {
        return (bakongAccountId == null || bakongAccountId.length() <= 32) &&
               (merchantName == null || merchantName.length() <= 25) &&
               (merchantCity == null || merchantCity.length() <= 15) &&
               (accountInformation == null || accountInformation.length() <= 32) &&
               (acquiringBank == null || acquiringBank.length() <= 32) &&
               (billNumber == null || billNumber.length() <= 25) &&
               (mobileNumber == null || mobileNumber.length() <= 25) &&
               (storeLabel == null || storeLabel.length() <= 25) &&
               (terminalLabel == null || terminalLabel.length() <= 25) &&
               (purposeOfTransaction == null || purposeOfTransaction.length() <= 25) &&
               (upiAccountInformation == null || upiAccountInformation.length() <= 31) &&
               (merchantNameAlternateLanguage == null || merchantNameAlternateLanguage.length() <= 25) &&
               (merchantCityAlternateLanguage == null || merchantCityAlternateLanguage.length() <= 15);
    }
}
