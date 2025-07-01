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
public class BakongPaymentRequestDTO {
    
    @JsonProperty("merchant_id")
    private String merchantId;
    
    @JsonProperty("merchant_transaction_id")
    private String merchantTransactionId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("callback_url")
    private String callbackUrl;
    
    @JsonProperty("return_url")
    private String returnUrl;
    
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    
    @JsonProperty("customer_info")
    private CustomerInfo customerInfo;
    
    @JsonProperty("metadata")
    private Object metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("phone")
        private String phone;
        
        @JsonProperty("email")
        private String email;
    }
}
