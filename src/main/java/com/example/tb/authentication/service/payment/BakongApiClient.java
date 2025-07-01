package com.example.tb.authentication.service.payment;

import com.example.tb.configuration.BakongConfig;
import com.example.tb.model.dto.bakong.BakongPaymentRequestDTO;
import com.example.tb.model.dto.bakong.BakongPaymentResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BakongApiClient {
    
    private final BakongConfig bakongConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String HMAC_SHA256 = "HmacSHA256";
      @Retryable(
        value = {Exception.class},
        maxAttemptsExpression = "#{@bakongConfig.api.retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "#{@bakongConfig.api.retry.delay}")
    )    public BakongPaymentResponseDTO createPayment(BakongPaymentRequestDTO request) {
        // Check if mock mode is explicitly enabled
        if (bakongConfig.getApi().isMockMode()) {
            log.info("Mock mode enabled - simulating Bakong payment for transaction: {}", 
                    request.getMerchantTransactionId());
            return createMockPaymentResponse(request);
        }
          try {
            // For Bakong, we'll use a simple payment endpoint structure
            String url = bakongConfig.getApi().getBaseUrl() + "/api/" + 
                        bakongConfig.getApi().getVersion() + "/payments";
            
            HttpHeaders headers = createHeaders(request);
            HttpEntity<BakongPaymentRequestDTO> entity = new HttpEntity<>(request, headers);
            
            log.info("Creating Bakong payment for transaction: {}", request.getMerchantTransactionId());
            
            ResponseEntity<BakongPaymentResponseDTO> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, BakongPaymentResponseDTO.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Bakong payment created successfully: {}", response.getBody().getTransactionId());
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to create Bakong payment: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error creating Bakong payment for transaction: {}", 
                     request.getMerchantTransactionId(), e);
            throw new RuntimeException("Bakong API error: " + e.getMessage(), e);
        }
    }
      @Retryable(
        value = {Exception.class},
        maxAttemptsExpression = "#{@bakongConfig.api.retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "#{@bakongConfig.api.retry.delay}")
    )    public BakongPaymentResponseDTO getPaymentStatus(String transactionId) {
        // Check if mock mode is explicitly enabled
        if (bakongConfig.getApi().isMockMode()) {
            log.debug("Mock mode enabled - simulating payment status check for transaction: {}", transactionId);
            return createMockStatusResponse(transactionId);
        }
          try {
            String url = bakongConfig.getApi().getBaseUrl() + "/api/" + 
                        bakongConfig.getApi().getVersion() + 
                        "/payments/" + transactionId + "/status";
            
            HttpHeaders headers = createHeaders(null);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            log.debug("Checking Bakong payment status for transaction: {}", transactionId);
            
            ResponseEntity<BakongPaymentResponseDTO> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, BakongPaymentResponseDTO.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get payment status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error getting Bakong payment status for transaction: {}", transactionId, e);
            throw new RuntimeException("Bakong API error: " + e.getMessage(), e);
        }
    }
    
    public boolean verifySignature(String payload, String signature) {
        try {
            String expectedSignature = generateSignature(payload);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error verifying Bakong signature", e);
            return false;
        }
    }
    
    private HttpHeaders createHeaders(Object requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Merchant-ID", bakongConfig.getApi().getMerchantId());
        headers.set("X-Timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        try {
            String payload = requestBody != null ? objectMapper.writeValueAsString(requestBody) : "";
            String signature = generateSignature(payload);
            headers.set("X-Signature", signature);
        } catch (Exception e) {
            log.error("Error creating headers", e);
            throw new RuntimeException("Failed to create request headers", e);
        }
        
        return headers;
    }    private String generateSignature(String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        String secretKey = bakongConfig.getApi().getSecretKey();
        if (secretKey == null || secretKey.trim().isEmpty() || secretKey.equals("your-bakong-secret-key-here")) {
            throw new IllegalStateException("Bakong secret key is not configured. Please set BAKONG_SECRET_KEY environment variable or configure secret-key in application.yml");
        }
        
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            secretKey.getBytes(StandardCharsets.UTF_8), 
            HMAC_SHA256
        );
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
    
    /**
     * Creates a mock payment response for development/testing when the real Bakong API is not accessible
     */    private BakongPaymentResponseDTO createMockPaymentResponse(BakongPaymentRequestDTO request) {
        BakongPaymentResponseDTO response = new BakongPaymentResponseDTO();
        response.setTransactionId("MOCK_TXN_" + System.currentTimeMillis());
        response.setMerchantTransactionId(request.getMerchantTransactionId());
        response.setStatus("PENDING");
        response.setAmount(request.getAmount());        response.setCurrency(request.getCurrency());        response.setQrCodeData("mock_qr_data_" + request.getMerchantTransactionId());
        response.setQrCodeUrl("https://api-bakong.nbc.gov.kh/mock/qr/" + request.getMerchantTransactionId());
        response.setPaymentUrl("https://api-bakong.nbc.gov.kh/mock/payment/" + response.getTransactionId());
        response.setDeepLinkUrl("https://api-bakong.nbc.gov.kh/mock/deeplink/" + response.getTransactionId());
        response.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        response.setCreatedAt(LocalDateTime.now());
        response.setMessage("Mock payment created successfully");
        
        log.info("Mock payment response created: txnId={}, status={}", 
                response.getTransactionId(), response.getStatus());
        
        return response;
    }
      /**
     * Creates a mock payment status response for development/testing
     */
    private BakongPaymentResponseDTO createMockStatusResponse(String transactionId) {
        BakongPaymentResponseDTO response = new BakongPaymentResponseDTO();
        response.setTransactionId(transactionId);
        
        // Simulate different payment statuses based on transaction ID patterns
        if (transactionId.contains("PENDING")) {
            response.setStatus("PENDING");
            response.setMessage("Payment is still pending");
        } else if (transactionId.contains("FAILED")) {
            response.setStatus("FAILED");
            response.setMessage("Payment failed");
            response.setErrorCode("PAYMENT_FAILED");
        } else {
            // Default to successful payment for testing
            response.setStatus("COMPLETED");
            response.setMessage("Payment completed successfully");
        }
        
        log.debug("Mock status response created: txnId={}, status={}", 
                response.getTransactionId(), response.getStatus());
        
        return response;
    }
}
