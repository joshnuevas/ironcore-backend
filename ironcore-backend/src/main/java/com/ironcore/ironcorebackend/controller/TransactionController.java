package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.entity.Transaction;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.repository.TransactionRepository;
import com.ironcore.ironcorebackend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TransactionController {

    private final TransactionService transactionService;
    
    @Autowired
    private TransactionRepository transactionRepository;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody TransactionRequest request) {
        try {
            Transaction savedTransaction = transactionService.createTransactionFromRequest(request);
            return ResponseEntity.ok(savedTransaction);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    // ⭐ NEW: Update transaction status
    @PutMapping("/{transactionId}/status")
    public ResponseEntity<?> updateTransactionStatus(
            @PathVariable Long transactionId,
            @RequestParam String status) {
        
        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            transaction.setPaymentStatus(PaymentStatus.valueOf(status));
            transactionRepository.save(transaction);
            
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to update transaction: " + e.getMessage());
        }
    }
}