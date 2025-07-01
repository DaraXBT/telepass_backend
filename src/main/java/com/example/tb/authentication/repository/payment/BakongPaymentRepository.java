package com.example.tb.authentication.repository.payment;

import com.example.tb.model.entity.BakongPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BakongPaymentRepository extends JpaRepository<BakongPayment, UUID> {
    
    Optional<BakongPayment> findByMerchantTransactionId(String merchantTransactionId);
    
    Optional<BakongPayment> findByBakongTransactionId(String bakongTransactionId);
    
    List<BakongPayment> findByEventId(UUID eventId);
    
    List<BakongPayment> findByUserId(UUID userId);
    
    List<BakongPayment> findByStatus(BakongPayment.PaymentStatus status);
    
    @Query("SELECT p FROM BakongPayment p WHERE p.eventId = :eventId AND p.userId = :userId")
    List<BakongPayment> findByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);
    
    @Query("SELECT p FROM BakongPayment p WHERE p.status = :status AND p.expiresAt < :currentTime")
    List<BakongPayment> findExpiredPayments(@Param("status") BakongPayment.PaymentStatus status, 
                                           @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT p FROM BakongPayment p WHERE p.userId = :userId AND p.status = 'COMPLETED' ORDER BY p.paidAt DESC")
    List<BakongPayment> findSuccessfulPaymentsByUser(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(p) FROM BakongPayment p WHERE p.eventId = :eventId AND p.status = 'COMPLETED'")
    long countSuccessfulPaymentsByEvent(@Param("eventId") UUID eventId);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM BakongPayment p WHERE p.eventId = :eventId AND p.status = 'COMPLETED'")
    java.math.BigDecimal getTotalRevenueByEvent(@Param("eventId") UUID eventId);
}
