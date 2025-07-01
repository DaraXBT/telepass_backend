package com.example.tb.model.dto.bakong;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request for generating event payment QR
 */
@Data
@Builder
public class EventPaymentRequest {
    private UUID eventId;
    private String eventName;
    private BigDecimal amount;
    private String currency;
    private String payerName;
    private String description;
}
