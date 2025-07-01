package com.example.tb.model.dto.bakong;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BakongPaymentResponseDTO {
    
    @JsonProperty("transaction_id")
    private String transactionId;
    
    @JsonProperty("merchant_transaction_id")
    private String merchantTransactionId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("qr_code_url")
    private String qrCodeUrl;
    
    @JsonProperty("qr_code_data")
    private String qrCodeData;
    
    @JsonProperty("deep_link_url")
    private String deepLinkUrl;
    
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("payment_url")
    private String paymentUrl;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("error_code")
    private String errorCode;
}
