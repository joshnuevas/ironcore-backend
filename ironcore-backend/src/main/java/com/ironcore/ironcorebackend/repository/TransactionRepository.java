package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Find transaction by transaction code
    Optional<Transaction> findByTransactionCode(String transactionCode);
    
    // Find transactions by user
    List<Transaction> findByUserId(Long userId);
}