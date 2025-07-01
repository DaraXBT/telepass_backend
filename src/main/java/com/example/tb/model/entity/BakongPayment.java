package com.example.tb.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bakong_payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BakongPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "merchant_transaction_id", unique = true, nullable = false)
    private String merchantTransactionId;
    
    @Column(name = "bakong_transaction_id")
    private String bakongTransactionId;
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "payer_name")
    private String payerName;
    
    @Column(name = "payer_phone")
    private String payerPhone;
    
    @Column(name = "payer_email")
    private String payerEmail;
    
    @Column(name = "event_id")
    private UUID eventId;
    
    @Column(name = "user_id")
    private UUID userId;
      @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;
    
    @Column(name = "deep_link_url")
    private String deepLinkUrl;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "callback_data", columnDefinition = "TEXT")
    private String callbackData;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        EXPIRED,
        REFUNDED
    }
    
    public enum PaymentMethod {
        BAKONG_WALLET,
        BANK_ACCOUNT,
        QR_CODE,
        DEEP_LINK
    }
}
