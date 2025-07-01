package com.example.tb.authentication.service.payment;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Using local stub implementation until official SDK is available in public repositories
// TODO: Replace with official SDK imports when available: kh.gov.nbc.bakong_khqr.*
import com.example.tb.bakong.sdk.*;
import com.example.tb.model.dto.bakong.EventPaymentRequest;
import com.example.tb.model.dto.bakong.KhqrGenerationResult;

import com.example.tb.configuration.BakongConfig;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Official Bakong KHQR Service using the official NBC SDK interface
 * 
 * IMPORTANT: Currently using stub implementation of the SDK classes
 * until the official Bakong KHQR SDK becomes available in public Maven repositories.
 * 
 * TODO: Replace stub classes with official SDK when available:
 * - Remove local stub classes from com.example.tb.bakong.sdk.*
 * - Add official dependency: implementation 'kh.gov.nbc.bakong_khqr:sdk-java:1.0.0'
 * - Update imports to: import kh.gov.nbc.bakong_khqr.*;
 * 
 * This service handles QR code generation, verification, and decoding using the
 * same interface as the official SDK, making the transition seamless when the
 * official SDK becomes available.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BakongKhqrService {
    
    private final BakongConfig bakongConfig;
    
    /**
     * Generate KHQR for event ticket payment using Merchant account
     */
    public KhqrGenerationResult generateEventPaymentQr(EventPaymentRequest request) {
        try {
            log.debug("Generating KHQR for event: {} with amount: {}", 
                request.getEventId(), request.getAmount());
            
            // Create merchant info for event payment
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setBakongAccountId(bakongConfig.getMerchant().getId());
            merchantInfo.setMerchantId(bakongConfig.getMerchant().getId());
            merchantInfo.setAcquiringBank(bakongConfig.getMerchant().getAcquiringBank());
            
            // Set currency based on configuration
            if ("USD".equalsIgnoreCase(request.getCurrency())) {
                merchantInfo.setCurrency(KHQRCurrency.USD);
            } else {
                merchantInfo.setCurrency(KHQRCurrency.KHR);
            }
            
            merchantInfo.setAmount(request.getAmount().doubleValue());
            merchantInfo.setMerchantName(bakongConfig.getMerchant().getName());
            merchantInfo.setMerchantCity(bakongConfig.getMerchant().getCity());
            
            // Optional event-specific fields
            merchantInfo.setBillNumber(request.getEventId().toString().substring(0, 
                Math.min(25, request.getEventId().toString().length()))); // Max 25 chars
            
            if (bakongConfig.getMerchant().getPhone() != null) {
                merchantInfo.setMobileNumber(bakongConfig.getMerchant().getPhone());
            }
            
            merchantInfo.setStoreLabel("Telepass Events");
            merchantInfo.setTerminalLabel("Event Reg");
            
            // Truncate description to max 25 characters
            String purpose = "Event: " + request.getEventName();
            if (purpose.length() > 25) {
                purpose = purpose.substring(0, 22) + "...";
            }
            merchantInfo.setPurposeOfTransaction(purpose);
            
            // Add Khmer language support if enabled
            if (bakongConfig.getQr().isEnableKhmerLanguage()) {
                merchantInfo.setMerchantAlternateLanguagePreference("km");
                merchantInfo.setMerchantNameAlternateLanguage("ការចុះឈ្មោះព្រឹត្តិការណ៍");
                merchantInfo.setMerchantCityAlternateLanguage("ភ្នំពេញ");
            }
            
            // Generate KHQR using official SDK
            KHQRResponse<KHQRData> response = BakongKHQR.generateMerchant(merchantInfo);
            
            if (response.getKHQRStatus().getCode() == 0) {
                log.info("Successfully generated KHQR for event: {}", request.getEventId());
                return KhqrGenerationResult.builder()
                    .success(true)
                    .qrCode(response.getData().getQr())
                    .md5Hash(response.getData().getMd5())
                    .build();
            } else {
                log.error("KHQR generation failed with code {}: {}", 
                    response.getKHQRStatus().getCode(), 
                    response.getKHQRStatus().getMessage());
                return KhqrGenerationResult.builder()
                    .success(false)
                    .errorCode(response.getKHQRStatus().getCode())
                    .errorMessage(response.getKHQRStatus().getMessage())
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error generating KHQR for event: " + request.getEventId(), e);
            return KhqrGenerationResult.builder()
                .success(false)
                .errorMessage("Internal error generating payment QR")
                .build();
        }
    }
    
    /**
     * Generate KHQR for individual payment (for testing or personal payments)
     */
    public KhqrGenerationResult generateIndividualPaymentQr(EventPaymentRequest request) {
        try {
            log.debug("Generating individual KHQR for event: {} with amount: {}", 
                request.getEventId(), request.getAmount());
            
            // Create individual info
            IndividualInfo individualInfo = new IndividualInfo();
            individualInfo.setBakongAccountId(bakongConfig.getMerchant().getId());
            individualInfo.setAccountInformation(bakongConfig.getMerchant().getPhone());
            individualInfo.setAcquiringBank(bakongConfig.getMerchant().getAcquiringBank());
            
            // Set currency
            if ("USD".equalsIgnoreCase(request.getCurrency())) {
                individualInfo.setCurrency(KHQRCurrency.USD);
            } else {
                individualInfo.setCurrency(KHQRCurrency.KHR);
            }
            
            individualInfo.setAmount(request.getAmount().doubleValue());
            individualInfo.setMerchantName(bakongConfig.getMerchant().getName());
            individualInfo.setMerchantCity(bakongConfig.getMerchant().getCity());
            
            // Optional fields
            individualInfo.setBillNumber(request.getEventId().toString().substring(0, 
                Math.min(25, request.getEventId().toString().length())));
            individualInfo.setMobileNumber(bakongConfig.getMerchant().getPhone());
            individualInfo.setStoreLabel("Telepass");
            individualInfo.setTerminalLabel("Event");
            
            String purpose = "Event: " + request.getEventName();
            if (purpose.length() > 25) {
                purpose = purpose.substring(0, 22) + "...";
            }
            individualInfo.setPurposeOfTransaction(purpose);
            
            // Generate KHQR
            KHQRResponse<KHQRData> response = BakongKHQR.generateIndividual(individualInfo);
            
            if (response.getKHQRStatus().getCode() == 0) {
                log.info("Successfully generated individual KHQR for event: {}", request.getEventId());
                return KhqrGenerationResult.builder()
                    .success(true)
                    .qrCode(response.getData().getQr())
                    .md5Hash(response.getData().getMd5())
                    .build();
            } else {
                log.error("Individual KHQR generation failed with code {}: {}", 
                    response.getKHQRStatus().getCode(), 
                    response.getKHQRStatus().getMessage());
                return KhqrGenerationResult.builder()
                    .success(false)
                    .errorCode(response.getKHQRStatus().getCode())
                    .errorMessage(response.getKHQRStatus().getMessage())
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error generating individual KHQR for event: " + request.getEventId(), e);
            return KhqrGenerationResult.builder()
                .success(false)
                .errorMessage("Internal error generating individual payment QR")
                .build();
        }
    }
    
    /**
     * Verify KHQR integrity using official SDK
     */
    public boolean verifyQrCode(String qrCode) {
        try {
            log.debug("Verifying KHQR integrity");
            KHQRResponse<CRCValidation> response = BakongKHQR.verify(qrCode);
            
            if (response.getKHQRStatus().getCode() == 0) {
                boolean isValid = response.getData().isValid();
                log.debug("KHQR verification result: {}", isValid);
                return isValid;
            } else {
                log.warn("KHQR verification failed with code {}: {}", 
                    response.getKHQRStatus().getCode(), 
                    response.getKHQRStatus().getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying KHQR", e);
            return false;
        }
    }
    
    /**
     * Decode KHQR to extract payment information
     */
    public Optional<KHQRDecodeData> decodeQrCode(String qrCode) {
        try {
            log.debug("Decoding KHQR");
            KHQRResponse<KHQRDecodeData> response = BakongKHQR.decode(qrCode);
            
            if (response.getKHQRStatus().getCode() == 0) {
                log.debug("Successfully decoded KHQR");
                return Optional.of(response.getData());
            } else {
                log.warn("KHQR decoding failed with code {}: {}", 
                    response.getKHQRStatus().getCode(), 
                    response.getKHQRStatus().getMessage());
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error decoding KHQR", e);
            return Optional.empty();
        }
    }
    
    /**
     * Generate deep link for Bakong app payment
     * Note: This requires a deep link API endpoint from NBC
     */
    public Optional<String> generateDeepLink(String qrCode, String appName, String callbackUrl) {
        try {
            log.debug("Generating deep link for KHQR");
            
            // This would require the deep link API endpoint from NBC
            // For now, create a simple Bakong app deep link
            String deepLink = "bakong://pay?qr=" + qrCode;
            
            if (callbackUrl != null) {
                deepLink += "&callback=" + java.net.URLEncoder.encode(callbackUrl, "UTF-8");
            }
            
            log.debug("Generated deep link for Bakong app");
            return Optional.of(deepLink);
            
        } catch (Exception e) {
            log.error("Error generating deep link", e);
            return Optional.empty();
        }
    }
    
    /**
     * Get error message for KHQR error code
     */
    public String getErrorMessage(int errorCode) {
        return switch (errorCode) {
            case 0 -> "Success";
            case 1 -> "Failed";
            case 2 -> "Bakong Account ID cannot be null or empty";
            case 3 -> "Merchant name cannot be null or empty";
            case 4 -> "Bakong Account ID is invalid";
            case 5 -> "Amount is invalid";
            case 6 -> "Merchant type cannot be null or empty";
            case 7 -> "Bakong Account ID Length is invalid";
            case 8 -> "Merchant Name Length is invalid";
            case 9 -> "KHQR provided is invalid";
            case 10 -> "Currency type cannot be null or empty";
            case 11 -> "Bill Number Length is invalid";
            case 12 -> "Store Label Length is invalid";
            case 13 -> "Terminal Label Length is invalid";
            case 14 -> "Cannot reach Bakong Open API service. Please check internet connection";
            case 15 -> "Source Info for Deep Link is invalid";
            case 16 -> "Internal Server Error";
            case 32 -> "Merchant ID cannot be null or empty";
            case 33 -> "Acquiring Bank cannot be null or empty";
            case 34 -> "Merchant ID Length is invalid";
            case 35 -> "Acquiring Bank Length is invalid";
            case 36 -> "Mobile Number Length is invalid";
            default -> "Unknown error code: " + errorCode;
        };
    }
}
