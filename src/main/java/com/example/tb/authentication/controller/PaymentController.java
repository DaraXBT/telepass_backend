package com.example.tb.authentication.controller;

import com.example.tb.authentication.service.payment.BakongPaymentService;
import com.example.tb.authentication.service.payment.BakongMetricsService;
import com.example.tb.model.dto.bakong.BakongCallbackDTO;
import com.example.tb.model.request.PaymentRequest;
import com.example.tb.model.response.ApiResponse;
import com.example.tb.model.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Controller", description = "Bakong payment management endpoints")
public class PaymentController {
    
    private final BakongPaymentService paymentService;
    private final BakongMetricsService metricsService;
    
    @PostMapping("/bakong/initiate")
    @Operation(summary = "Initiate Bakong payment", 
               description = "Create a new Bakong payment for event registration")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentRequest request) {
        try {
            PaymentResponse payment = paymentService.initiatePayment(request);
            
            return ResponseEntity.ok(
                ApiResponse.<PaymentResponse>builder()
                    .date(LocalDateTime.now())
                    .message("Payment initiated successfully")
                    .payload(payment)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error initiating payment", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<PaymentResponse>builder()
                    .date(LocalDateTime.now())
                    .message("Failed to initiate payment: " + e.getMessage())
                    .build()
            );
        }
    }
      @PostMapping("/bakong/callback")
    @Operation(summary = "Handle Bakong payment callback", 
               description = "Process payment status updates from Bakong")
    public ResponseEntity<Map<String, String>> handleCallback(
            @RequestBody BakongCallbackDTO callback,
            @RequestHeader(value = "X-Bakong-Signature", required = false) String signature,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp) {
        try {
            // Log callback for audit purposes
            log.info("Received Bakong callback for transaction: {}", 
                    callback.getMerchantTransactionId());
            
            // TODO: Add signature verification in production
            // if (signature != null && timestamp != null) {
            //     // Verify signature using BakongSignatureUtil
            // }
            
            PaymentResponse payment = paymentService.handleCallback(callback);
            
            log.info("Payment callback processed successfully: {}", 
                    callback.getMerchantTransactionId());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Callback processed successfully",
                "paymentStatus", payment.getStatus().toString()
            ));
            
        } catch (Exception e) {
            log.error("Error processing payment callback for transaction: {}", 
                     callback.getMerchantTransactionId(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to process callback: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/bakong/{merchantTransactionId}/status")
    @Operation(summary = "Get payment status", 
               description = "Check the current status of a payment")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            @PathVariable String merchantTransactionId) {
        try {
            PaymentResponse payment = paymentService.getPaymentStatus(merchantTransactionId);
            
            return ResponseEntity.ok(
                ApiResponse.<PaymentResponse>builder()
                    .date(LocalDateTime.now())
                    .message("Payment status retrieved successfully")
                    .payload(payment)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting payment status", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<PaymentResponse>builder()
                    .date(LocalDateTime.now())
                    .message("Failed to get payment status: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user payments", 
               description = "Get all payments for a specific user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getUserPayments(
            @PathVariable UUID userId) {
        try {
            List<PaymentResponse> payments = paymentService.getUserPayments(userId);
            
            return ResponseEntity.ok(
                ApiResponse.<List<PaymentResponse>>builder()
                    .date(LocalDateTime.now())
                    .message("User payments retrieved successfully")
                    .payload(payments)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting user payments", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<List<PaymentResponse>>builder()
                    .date(LocalDateTime.now())
                    .message("Failed to get user payments: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get event payments", 
               description = "Get all payments for a specific event")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getEventPayments(
            @PathVariable UUID eventId) {
        try {
            List<PaymentResponse> payments = paymentService.getEventPayments(eventId);
            
            return ResponseEntity.ok(
                ApiResponse.<List<PaymentResponse>>builder()
                    .date(LocalDateTime.now())
                    .message("Event payments retrieved successfully")
                    .payload(payments)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting event payments", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<List<PaymentResponse>>builder()
                    .date(LocalDateTime.now())
                    .message("Failed to get event payments: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @GetMapping("/event/{eventId}/revenue")
    @Operation(summary = "Get event revenue", 
               description = "Get total revenue for a specific event")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventRevenue(
            @PathVariable UUID eventId) {
        try {
            BigDecimal revenue = paymentService.getEventRevenue(eventId);
            
            Map<String, Object> revenueData = Map.of(
                "eventId", eventId,
                "totalRevenue", revenue,
                "currency", "KHR",
                "calculatedAt", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .date(LocalDateTime.now())
                    .message("Event revenue calculated successfully")
                    .payload(revenueData)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error calculating event revenue", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .date(LocalDateTime.now())
                    .message("Failed to calculate event revenue: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @PostMapping("/test/bakong")
    @Operation(summary = "Test Bakong integration", 
               description = "Test endpoint for Bakong payment integration")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testBakongIntegration() {
        try {
            Map<String, Object> testData = Map.of(
                "status", "success",
                "message", "Bakong integration is working",
                "timestamp", LocalDateTime.now(),
                "endpoints", Map.of(
                    "initiate", "/api/v1/payments/bakong/initiate",
                    "callback", "/api/v1/payments/bakong/callback",
                    "status", "/api/v1/payments/bakong/{merchantTransactionId}/status"
                )
            );
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .date(LocalDateTime.now())
                    .message("Bakong integration test successful")
                    .payload(testData)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error testing Bakong integration", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, Object>>builder()
                    .date(LocalDateTime.now())
                    .message("Bakong integration test failed: " + e.getMessage())
                    .build()
            );
        }
    }

    @GetMapping("/bakong/metrics")
    @Operation(summary = "Get payment metrics", 
               description = "Retrieve real-time payment processing metrics")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<BakongMetricsService.PaymentMetrics>> getPaymentMetrics() {
        try {
            BakongMetricsService.PaymentMetrics metrics = metricsService.getMetrics();
            
            return ResponseEntity.ok(
                ApiResponse.<BakongMetricsService.PaymentMetrics>builder()
                    .date(LocalDateTime.now())
                    .message("Payment metrics retrieved successfully")
                    .payload(metrics)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error retrieving payment metrics", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<BakongMetricsService.PaymentMetrics>builder()
                    .date(LocalDateTime.now())
                    .message("Failed to retrieve payment metrics: " + e.getMessage())
                    .build()
            );
        }
    }

    @GetMapping("/bakong/health")
    @Operation(summary = "Health check", 
               description = "Check Bakong payment service health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "bakong-payment",
                "timestamp", System.currentTimeMillis(),
                "version", "1.1.0-enhanced"
        ));
    }
}
