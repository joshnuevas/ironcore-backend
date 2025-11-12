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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    // Update transaction status
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

    // ⭐ NEW: Check if user has active enrollment for a class (prevent duplicate bookings)
    @GetMapping("/check-active-enrollment")
    public ResponseEntity<?> checkActiveEnrollment(
            @RequestParam Long userId,
            @RequestParam Long classId) {
        try {
            Optional<Transaction> activeEnrollment = 
                transactionRepository.findActiveEnrollmentByUserAndClass(userId, classId);
            
            if (activeEnrollment.isPresent()) {
                Transaction transaction = activeEnrollment.get();
                Map<String, Object> response = new HashMap<>();
                response.put("hasActiveEnrollment", true);
                response.put("className", transaction.getClassName());
                response.put("scheduleDay", transaction.getScheduleDay());
                response.put("scheduleTime", transaction.getScheduleTime());
                response.put("scheduleDate", transaction.getScheduleDate());
                response.put("paymentStatus", transaction.getPaymentStatus().toString());
                response.put("transactionCode", transaction.getTransactionCode());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("hasActiveEnrollment", false);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking enrollment: " + e.getMessage());
        }
    }

    // ⭐ NEW: Check if user has active membership (prevent duplicate membership purchases)
    @GetMapping("/check-active-membership")
    public ResponseEntity<?> checkActiveMembership(@RequestParam Long userId) {
        try {
            Optional<Transaction> activeMembership = 
                transactionRepository.findActiveMembershipByUser(userId);
            
            if (activeMembership.isPresent()) {
                Transaction transaction = activeMembership.get();
                Map<String, Object> response = new HashMap<>();
                response.put("hasActiveMembership", true);
                response.put("membershipType", transaction.getMembershipType());
                response.put("paymentStatus", transaction.getPaymentStatus().toString());
                response.put("paymentDate", transaction.getPaymentDate());
                response.put("transactionCode", transaction.getTransactionCode());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("hasActiveMembership", false);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking membership: " + e.getMessage());
        }
    }

    // ⭐ NEW: Mark a session as completed
    @PutMapping("/{transactionId}/complete-session")
    public ResponseEntity<?> completeSession(@PathVariable Long transactionId) {
        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            transaction.setSessionCompleted(true);
            transactionRepository.save(transaction);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Session marked as completed");
            response.put("transactionCode", transaction.getTransactionCode());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to complete session: " + e.getMessage());
        }
    }

    // Check transaction code validity
    @GetMapping("/check/{transactionCode}")
    public ResponseEntity<Map<String, Object>> checkTransactionCode(@PathVariable String transactionCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Transaction transaction = transactionRepository
                .findByTransactionCode(transactionCode)
                .orElse(null);
            
            if (transaction == null) {
                response.put("valid", false);
                response.put("message", "Transaction code not found");
                return ResponseEntity.ok(response);
            }
            
            boolean isPaid = transaction.getPaymentStatus() == PaymentStatus.COMPLETED 
                          || transaction.getPaymentStatus() == PaymentStatus.PAID;
            
            response.put("valid", isPaid);
            response.put("transaction", transaction);
            response.put("userName", transaction.getUser().getUsername());
            response.put("userEmail", transaction.getUserEmail());
            response.put("paymentStatus", transaction.getPaymentStatus().toString());
            response.put("totalAmount", transaction.getTotalAmount());
            response.put("paymentDate", transaction.getPaymentDate());
            response.put("sessionCompleted", transaction.getSessionCompleted());
            
            // Include class/membership info
            if (transaction.getClassName() != null) {
                response.put("type", "CLASS");
                response.put("className", transaction.getClassName());
                response.put("scheduleDay", transaction.getScheduleDay());
                response.put("scheduleTime", transaction.getScheduleTime());
                response.put("scheduleDate", transaction.getScheduleDate());
            } else if (transaction.getMembershipType() != null) {
                response.put("type", "MEMBERSHIP");
                response.put("membershipType", transaction.getMembershipType());
            }
            
            if (isPaid) {
                if (transaction.getSessionCompleted()) {
                    response.put("message", "✅ Valid - Session completed");
                } else {
                    response.put("message", "✅ Valid - Access granted");
                }
            } else {
                response.put("message", "❌ Payment not completed");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("valid", false);
            response.put("message", "Error checking transaction: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get all transactions for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable Long userId) {
        try {
            List<Transaction> transactions = transactionRepository.findByUserId(userId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}