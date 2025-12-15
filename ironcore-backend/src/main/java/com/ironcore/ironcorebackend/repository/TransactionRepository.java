package com.ironcore.ironcorebackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionCode(String transactionCode);

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByPaymentStatus(PaymentStatus paymentStatus);
    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("""
    SELECT COUNT(ce) > 0
    FROM ClassEnrollment ce
    WHERE ce.user.id = :userId
        AND ce.schedule.id = :scheduleId
        AND ce.transaction.paymentStatus IN (
        com.ironcore.ironcorebackend.entity.PaymentStatus.PENDING,
        com.ironcore.ironcorebackend.entity.PaymentStatus.COMPLETED
        )
        AND (ce.sessionCompleted = false OR ce.sessionCompleted IS NULL)
    """)
    boolean existsBookedEnrollmentForSchedule(
        @Param("userId") Long userId,
        @Param("scheduleId") Long scheduleId
    );

}