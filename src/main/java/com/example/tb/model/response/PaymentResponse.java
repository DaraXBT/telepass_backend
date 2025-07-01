package com.example.tb.model.response;

import com.example.tb.model.entity.BakongPayment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private UUID id;
    private String merchantTransactionId;
    private String bakongTransactionId;
    private BigDecimal amount;
    private String currency;
    private BakongPayment.PaymentStatus status;
    private BakongPayment.PaymentMethod paymentMethod;
    private String description;
    private String payerName;
    private String payerPhone;
    private String payerEmail;
    private UUID eventId;
    private UUID userId;    private String qrCodeUrl;
    private String qrCodeData;
    private String deepLinkUrl;
    private String paymentUrl;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String errorMessage;
    
    public static PaymentResponse fromEntity(BakongPayment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .merchantTransactionId(payment.getMerchantTransactionId())
                .bakongTransactionId(payment.getBakongTransactionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .description(payment.getDescription())
                .payerName(payment.getPayerName())
                .payerPhone(payment.getPayerPhone())
                .payerEmail(payment.getPayerEmail())
                .eventId(payment.getEventId())
                .userId(payment.getUserId())                .qrCodeUrl(payment.getQrCodeUrl())
                .qrCodeData(payment.getQrCodeData())
                .deepLinkUrl(payment.getDeepLinkUrl())
                .expiresAt(payment.getExpiresAt())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .errorMessage(payment.getErrorMessage())
                .build();
    }
}
