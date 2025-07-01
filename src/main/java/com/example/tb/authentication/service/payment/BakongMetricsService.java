package com.example.tb.authentication.service.payment;

import com.example.tb.configuration.BakongConfig;
import com.example.tb.model.entity.BakongPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Payment metrics and monitoring service for Bakong integration
 * Provides real-time metrics and alerting capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "bakong.monitoring.metrics-enabled", havingValue = "true", matchIfMissing = true)
public class BakongMetricsService {
    
    private final BakongConfig bakongConfig;
    
    // Metrics counters
    private final AtomicInteger totalTransactions = new AtomicInteger(0);
    private final AtomicInteger successfulTransactions = new AtomicInteger(0);
    private final AtomicInteger failedTransactions = new AtomicInteger(0);
    private final AtomicInteger pendingTransactions = new AtomicInteger(0);
    private final AtomicLong totalAmount = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicInteger responseTimeCount = new AtomicInteger(0);
    
    /**
     * Record payment initiation
     */
    public void recordPaymentInitiated(BakongPayment payment) {
        if (!bakongConfig.getMonitoring().isMetricsEnabled()) {
            return;
        }
        
        totalTransactions.incrementAndGet();
        pendingTransactions.incrementAndGet();
        
        log.info("Payment initiated - Transaction: {}, Amount: {} {}", 
                payment.getMerchantTransactionId(), 
                payment.getAmount(), 
                payment.getCurrency());
    }
    
    /**
     * Record payment completion
     */
    public void recordPaymentCompleted(BakongPayment payment) {
        if (!bakongConfig.getMonitoring().isMetricsEnabled()) {
            return;
        }
        
        successfulTransactions.incrementAndGet();
        pendingTransactions.decrementAndGet();
        totalAmount.addAndGet(payment.getAmount().longValue());
        
        log.info("Payment completed - Transaction: {}, Amount: {} {}", 
                payment.getMerchantTransactionId(), 
                payment.getAmount(), 
                payment.getCurrency());
        
        // Check for alerts
        checkAlertThresholds();
    }
    
    /**
     * Record payment failure
     */
    public void recordPaymentFailed(BakongPayment payment, String reason) {
        if (!bakongConfig.getMonitoring().isMetricsEnabled()) {
            return;
        }
        
        failedTransactions.incrementAndGet();
        pendingTransactions.decrementAndGet();
        
        log.warn("Payment failed - Transaction: {}, Reason: {}", 
                payment.getMerchantTransactionId(), reason);
        
        // Check for alerts
        checkAlertThresholds();
    }
    
    /**
     * Record API response time
     */
    public void recordResponseTime(long responseTimeMs) {
        if (!bakongConfig.getMonitoring().isMetricsEnabled()) {
            return;
        }
        
        totalResponseTime.addAndGet(responseTimeMs);
        responseTimeCount.incrementAndGet();
        
        // Check response time threshold
        if (responseTimeMs > bakongConfig.getMonitoring().getAlertThresholds().getResponseTime()) {
            log.warn("High response time detected: {}ms", responseTimeMs);
        }
    }
    
    /**
     * Get current success rate
     */
    public double getSuccessRate() {
        int total = totalTransactions.get();
        if (total == 0) {
            return 1.0;
        }
        return (double) successfulTransactions.get() / total;
    }
    
    /**
     * Get current failure rate
     */
    public double getFailureRate() {
        int total = totalTransactions.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) failedTransactions.get() / total;
    }
    
    /**
     * Get average response time
     */
    public double getAverageResponseTime() {
        int count = responseTimeCount.get();
        if (count == 0) {
            return 0.0;
        }
        return (double) totalResponseTime.get() / count;
    }
    
    /**
     * Get total revenue
     */
    public BigDecimal getTotalRevenue() {
        return BigDecimal.valueOf(totalAmount.get());
    }
    
    /**
     * Get metrics summary
     */
    public PaymentMetrics getMetrics() {
        return PaymentMetrics.builder()
                .totalTransactions(totalTransactions.get())
                .successfulTransactions(successfulTransactions.get())
                .failedTransactions(failedTransactions.get())
                .pendingTransactions(pendingTransactions.get())
                .successRate(getSuccessRate())
                .failureRate(getFailureRate())
                .averageResponseTime(getAverageResponseTime())
                .totalRevenue(getTotalRevenue())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Reset metrics (useful for testing)
     */
    public void resetMetrics() {
        totalTransactions.set(0);
        successfulTransactions.set(0);
        failedTransactions.set(0);
        pendingTransactions.set(0);
        totalAmount.set(0);
        totalResponseTime.set(0);
        responseTimeCount.set(0);
        
        log.info("Payment metrics reset");
    }
    
    /**
     * Check alert thresholds and log warnings
     */
    private void checkAlertThresholds() {
        var thresholds = bakongConfig.getMonitoring().getAlertThresholds();
        
        // Check failure rate
        double failureRate = getFailureRate();
        if (failureRate > thresholds.getFailureRate()) {
            log.error("ALERT: High failure rate detected: {:.2%} (threshold: {:.2%})", 
                     failureRate, thresholds.getFailureRate());
        }
        
        // Check concurrent transactions
        int pending = pendingTransactions.get();
        if (pending > thresholds.getConcurrentTransactions()) {
            log.warn("ALERT: High concurrent transactions: {} (threshold: {})", 
                    pending, thresholds.getConcurrentTransactions());
        }
    }
    
    /**
     * Metrics data transfer object
     */
    @lombok.Data
    @lombok.Builder
    public static class PaymentMetrics {
        private int totalTransactions;
        private int successfulTransactions;
        private int failedTransactions;
        private int pendingTransactions;
        private double successRate;
        private double failureRate;
        private double averageResponseTime;
        private BigDecimal totalRevenue;
        private LocalDateTime timestamp;
    }
}
