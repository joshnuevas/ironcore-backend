package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // ⭐ NEW: Find transaction by unique transaction code
    Optional<Transaction> findByTransactionCode(String transactionCode);
}