package com.example.tb.authentication.service.payment;

import com.example.tb.authentication.repository.payment.BakongPaymentRepository;
import com.example.tb.authentication.service.event.EventService;
import com.example.tb.configuration.BakongConfig;
import com.example.tb.model.dto.bakong.BakongCallbackDTO;
import com.example.tb.model.dto.bakong.BakongPaymentRequestDTO;
import com.example.tb.model.dto.bakong.BakongPaymentResponseDTO;
import com.example.tb.model.dto.bakong.EventPaymentRequest;
import com.example.tb.model.dto.bakong.KhqrGenerationResult;
import com.example.tb.model.entity.BakongPayment;
import com.example.tb.model.request.PaymentRequest;
import com.example.tb.model.response.EventResponse;
import com.example.tb.model.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BakongPaymentService {
    
    private final BakongPaymentRepository paymentRepository;
    private final BakongApiClient bakongApiClient;
    private final BakongKhqrService bakongKhqrService; // Add KHQR service
    private final EventService eventService;
    private final BakongConfig bakongConfig;
    
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        try {
            // Validate event exists
            Optional<EventResponse> event = eventService.getEventById(request.getEventId());
            if (event.isEmpty()) {
                throw new RuntimeException("Event not found");
            }
            
            // Generate unique merchant transaction ID
            String merchantTransactionId = generateMerchantTransactionId();
            
            // Create payment entity
            BakongPayment payment = BakongPayment.builder()
                    .merchantTransactionId(merchantTransactionId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(BakongPayment.PaymentStatus.PENDING)
                    .description(request.getDescription())
                    .payerName(request.getPayerName())
                    .payerPhone(request.getPayerPhone())
                    .payerEmail(request.getPayerEmail())
                    .eventId(request.getEventId())
                    .userId(request.getUserId())
                    .expiresAt(LocalDateTime.now().plusMinutes(bakongConfig.getPayment().getExpiryMinutes()))
                    .build();
            
            payment = paymentRepository.save(payment);
            
            // Create Bakong payment request
            BakongPaymentRequestDTO bakongRequest = BakongPaymentRequestDTO.builder()
                    .merchantId(bakongConfig.getApi().getMerchantId())
                    .merchantTransactionId(merchantTransactionId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .description(request.getDescription())
                    .callbackUrl(bakongConfig.getApi().getCallbackUrl())
                    .returnUrl(request.getReturnUrl() != null ? request.getReturnUrl() : bakongConfig.getApi().getReturnUrl())
                    .expiresAt(payment.getExpiresAt())
                    .customerInfo(BakongPaymentRequestDTO.CustomerInfo.builder()
                            .name(request.getPayerName())
                            .phone(request.getPayerPhone())                            .email(request.getPayerEmail())
                            .build())
                    .metadata(createMetadata(request))
                    .build();
              // Generate KHQR using the official SDK service
            EventPaymentRequest khqrRequest = EventPaymentRequest.builder()
                    .eventId(request.getEventId())
                    .eventName(event.get().getName())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .payerName(request.getPayerName())
                    .description(request.getDescription())
                    .build();
              KhqrGenerationResult khqrResult = bakongKhqrService.generateEventPaymentQr(khqrRequest);
              if (khqrResult.isSuccess()) {
                // Update payment with KHQR data
                payment.setQrCodeUrl("https://api-bakong.nbc.gov.kh/qr/" + khqrResult.getMd5Hash());
                payment.setQrCodeData(khqrResult.getQrCode()); // Store the raw QR code data
                payment.setDeepLinkUrl("bakong://pay?qr=" + khqrResult.getMd5Hash());
                payment.setStatus(BakongPayment.PaymentStatus.PENDING);
                
                // Also call the API client for additional functionality (status tracking, etc.)
                try {
                    BakongPaymentResponseDTO bakongResponse = bakongApiClient.createPayment(bakongRequest);
                    payment.setBakongTransactionId(bakongResponse.getTransactionId());
                    if (bakongResponse.getErrorCode() != null) {
                        payment.setErrorMessage(bakongResponse.getMessage());
                    }
                } catch (Exception apiEx) {
                    log.warn("Bakong API call failed, but KHQR generated successfully: {}", apiEx.getMessage());
                    // Continue with KHQR-only payment
                    payment.setBakongTransactionId("KHQR_" + merchantTransactionId);
                }
            } else {
                // Fallback to API client if KHQR generation fails
                log.warn("KHQR generation failed: {}, falling back to API client", khqrResult.getErrorMessage());
                BakongPaymentResponseDTO bakongResponse = bakongApiClient.createPayment(bakongRequest);
                
                payment.setBakongTransactionId(bakongResponse.getTransactionId());
                payment.setQrCodeUrl(bakongResponse.getQrCodeUrl());
                payment.setDeepLinkUrl(bakongResponse.getDeepLinkUrl());
                payment.setStatus(mapBakongStatus(bakongResponse.getStatus()));
                
                if (bakongResponse.getErrorCode() != null) {
                    payment.setErrorMessage(bakongResponse.getMessage());
                    payment.setStatus(BakongPayment.PaymentStatus.FAILED);
                }            }
            
            payment = paymentRepository.save(payment);
            
            log.info("Payment initiated successfully: {} -> {}", 
                    merchantTransactionId, payment.getBakongTransactionId());
            
            PaymentResponse response = PaymentResponse.fromEntity(payment);
            response.setPaymentUrl(payment.getQrCodeUrl()); // Use QR code URL as payment URL
            
            return response;
            
        } catch (Exception e) {
            log.error("Error initiating payment for user {} and event {}", 
                     request.getUserId(), request.getEventId(), e);
            throw new RuntimeException("Failed to initiate payment: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public PaymentResponse handleCallback(BakongCallbackDTO callback) {
        try {
            // Find payment by merchant transaction ID
            Optional<BakongPayment> paymentOpt = paymentRepository
                    .findByMerchantTransactionId(callback.getMerchantTransactionId());
            
            if (paymentOpt.isEmpty()) {
                throw new RuntimeException("Payment not found: " + callback.getMerchantTransactionId());
            }
            
            BakongPayment payment = paymentOpt.get();
            
            // Update payment status
            payment.setBakongTransactionId(callback.getTransactionId());
            payment.setStatus(mapBakongStatus(callback.getStatus()));
            
            if ("COMPLETED".equalsIgnoreCase(callback.getStatus())) {
                payment.setPaidAt(callback.getPaidAt());
            }
            
            // Store callback data for audit
            payment.setCallbackData(callback.toString());
            
            payment = paymentRepository.save(payment);
            
            log.info("Payment callback processed: {} -> {}", 
                    callback.getMerchantTransactionId(), callback.getStatus());
            
            // Process post-payment actions asynchronously
            if (payment.getStatus() == BakongPayment.PaymentStatus.COMPLETED) {
                processSuccessfulPayment(payment);
            }
            
            return PaymentResponse.fromEntity(payment);
            
        } catch (Exception e) {
            log.error("Error processing payment callback", e);
            throw new RuntimeException("Failed to process payment callback: " + e.getMessage(), e);
        }
    }
    
    public PaymentResponse getPaymentStatus(String merchantTransactionId) {
        Optional<BakongPayment> payment = paymentRepository
                .findByMerchantTransactionId(merchantTransactionId);
        
        if (payment.isEmpty()) {
            throw new RuntimeException("Payment not found: " + merchantTransactionId);
        }
        
        return PaymentResponse.fromEntity(payment.get());
    }
    
    public List<PaymentResponse> getUserPayments(UUID userId) {
        List<BakongPayment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }
    
    public List<PaymentResponse> getEventPayments(UUID eventId) {
        List<BakongPayment> payments = paymentRepository.findByEventId(eventId);
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }
    
    public BigDecimal getEventRevenue(UUID eventId) {
        return paymentRepository.getTotalRevenueByEvent(eventId);
    }
    
    @Async
    protected void processSuccessfulPayment(BakongPayment payment) {
        try {
            // Here you can add post-payment processing logic
            // For example:
            // - Send confirmation emails
            // - Update event registration
            // - Generate tickets/QR codes
            // - Update analytics
            
            log.info("Processing successful payment: {}", payment.getMerchantTransactionId());
            
            // You could integrate with your existing event registration system
            // eventService.confirmRegistration(payment.getEventId(), payment.getUserId());
            
        } catch (Exception e) {
            log.error("Error processing successful payment: {}", payment.getMerchantTransactionId(), e);
        }
    }
    
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void expireOldPayments() {
        try {
            List<BakongPayment> expiredPayments = paymentRepository.findExpiredPayments(
                    BakongPayment.PaymentStatus.PENDING, LocalDateTime.now()
            );
            
            for (BakongPayment payment : expiredPayments) {
                payment.setStatus(BakongPayment.PaymentStatus.EXPIRED);
                paymentRepository.save(payment);
                log.info("Expired payment: {}", payment.getMerchantTransactionId());
            }
            
        } catch (Exception e) {
            log.error("Error expiring old payments", e);
        }
    }
    
    private String generateMerchantTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private BakongPayment.PaymentStatus mapBakongStatus(String bakongStatus) {
        if (bakongStatus == null) return BakongPayment.PaymentStatus.PENDING;
        
        return switch (bakongStatus.toUpperCase()) {
            case "PENDING" -> BakongPayment.PaymentStatus.PENDING;
            case "PROCESSING" -> BakongPayment.PaymentStatus.PROCESSING;
            case "COMPLETED", "SUCCESS" -> BakongPayment.PaymentStatus.COMPLETED;
            case "FAILED", "ERROR" -> BakongPayment.PaymentStatus.FAILED;
            case "CANCELLED" -> BakongPayment.PaymentStatus.CANCELLED;
            case "EXPIRED" -> BakongPayment.PaymentStatus.EXPIRED;
            case "REFUNDED" -> BakongPayment.PaymentStatus.REFUNDED;
            default -> BakongPayment.PaymentStatus.PENDING;
        };
    }
    
    private Object createMetadata(PaymentRequest request) {
        return Map.of(
                "eventId", request.getEventId(),
                "userId", request.getUserId(),
                "timestamp", LocalDateTime.now(),
                "source", "telepass_app"
        );
    }
}
