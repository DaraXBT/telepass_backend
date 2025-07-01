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
public class BakongCallbackDTO {
    
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
    
    @JsonProperty("paid_at")
    private LocalDateTime paidAt;
    
    @JsonProperty("payer_info")
    private PayerInfo payerInfo;
    
    @JsonProperty("signature")
    private String signature;
    
    @JsonProperty("metadata")
    private Object metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayerInfo {
        @JsonProperty("account_id")
        private String accountId;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("phone")
        private String phone;
    }
}
