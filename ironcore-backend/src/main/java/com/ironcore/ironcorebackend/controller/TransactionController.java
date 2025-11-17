package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.Transaction;
import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{transactionCode}")
    public ResponseEntity<Transaction> getTransactionByCode(@PathVariable String transactionCode) {
        Optional<Transaction> transaction = transactionService.getTransactionByCode(transactionCode);
        return transaction.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> getTransactionsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }

    @PutMapping("/{transactionId}/status")
    public ResponseEntity<Transaction> updateTransactionStatus(
            @PathVariable Long transactionId,
            @RequestParam String status
    ) {
        try {
            Transaction updatedTransaction = transactionService.updateTransactionStatus(transactionId, status);
            return ResponseEntity.ok(updatedTransaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/check-active-membership/{userId}")
    public ResponseEntity<Boolean> checkActiveMembership(@PathVariable Long userId) {
        boolean hasActive = transactionService.hasActiveMembership(userId);
        return ResponseEntity.ok(hasActive);
    }

    @GetMapping("/check-active-enrollment")
    public ResponseEntity<Boolean> checkActiveEnrollment(
            @RequestParam Long userId,
            @RequestParam Long classId
    ) {
        boolean hasActive = transactionService.hasActiveEnrollment(userId, classId);
        return ResponseEntity.ok(hasActive);
    }
}
