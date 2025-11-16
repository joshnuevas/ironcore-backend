package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.Transaction;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Find transaction by transaction code
    Optional<Transaction> findByTransactionCode(String transactionCode);
    
    // Find transactions by user
    List<Transaction> findByUserId(Long userId);
    
    // Check if user has an active (not completed) enrollment for a specific class
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.classEntity.id = :classId " +
           "AND t.sessionCompleted = false " +
           "AND (t.paymentStatus = 'COMPLETED' OR t.paymentStatus = 'PAID')")
    Optional<Transaction> findActiveEnrollmentByUserAndClass(
        @Param("userId") Long userId, 
        @Param("classId") Long classId
    );
    
    // ⭐ FIXED: Only find membership PURCHASE transactions (not class enrollments)
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.membershipType IN ('SILVER', 'GOLD', 'PLATINUM', 'SESSION') " +
           "AND t.className IS NULL " +
           "AND (t.paymentStatus = 'COMPLETED' OR t.paymentStatus = 'PAID') " +
           "AND t.membershipActivatedDate IS NOT NULL " +
           "AND t.membershipExpiryDate > :currentDate " +
           "ORDER BY t.paymentDate DESC")
    List<Transaction> findActiveMembershipsByUser(
        @Param("userId") Long userId,
        @Param("currentDate") LocalDateTime currentDate
    );
    
    // Find transactions by payment status
    List<Transaction> findByPaymentStatus(PaymentStatus paymentStatus);
    
    // Increment enrolled count for a schedule
    @Transactional
    @Modifying
    @Query("UPDATE Schedule s SET s.enrolledCount = s.enrolledCount + 1 WHERE s.id = :scheduleId")
    void incrementEnrolledCount(@Param("scheduleId") Long scheduleId);
}