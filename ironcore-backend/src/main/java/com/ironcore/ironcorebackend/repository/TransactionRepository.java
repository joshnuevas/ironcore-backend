package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.Transaction;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionCode(String transactionCode);

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByPaymentStatus(PaymentStatus paymentStatus);
    long countByPaymentStatus(PaymentStatus paymentStatus);

}
