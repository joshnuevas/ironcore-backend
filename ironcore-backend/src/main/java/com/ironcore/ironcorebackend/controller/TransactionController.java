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

import java.time.LocalDateTime;
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
    public ResponseEntity<?> createTransaction(@RequestBody TransactionRequest request) {
        try {
            // ⭐ CHECK 1: Check for active membership before creating transaction
            if (request.getMembershipType() != null && !request.getMembershipType().isEmpty()) {
                List<Transaction> activeMemberships = 
                    transactionRepository.findActiveMembershipsByUser(
                        request.getUserId(), 
                        LocalDateTime.now()
                    );
                
                if (!activeMemberships.isEmpty()) {
                    Transaction existing = activeMemberships.get(0);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "ACTIVE_MEMBERSHIP_EXISTS");
                    errorResponse.put("message", "You already have an active " + existing.getMembershipType() + " membership");
                    errorResponse.put("membershipType", existing.getMembershipType());
                    errorResponse.put("membershipActivatedDate", existing.getMembershipActivatedDate());
                    errorResponse.put("membershipExpiryDate", existing.getMembershipExpiryDate());
                    errorResponse.put("transactionCode", existing.getTransactionCode());
                    
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }
            }
            
            // ⭐ CHECK 2: Check for active class enrollment before creating transaction
            if (request.getClassId() != null) {
                Optional<Transaction> activeEnrollment = 
                    transactionRepository.findActiveEnrollmentByUserAndClass(
                        request.getUserId(), 
                        request.getClassId()
                    );
                
                if (activeEnrollment.isPresent()) {
                    Transaction existing = activeEnrollment.get();
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "ACTIVE_ENROLLMENT_EXISTS");
                    errorResponse.put("message", "You already have an active enrollment for this class");
                    errorResponse.put("className", existing.getClassName());
                    errorResponse.put("scheduleDay", existing.getScheduleDay());
                    errorResponse.put("scheduleTime", existing.getScheduleTime());
                    errorResponse.put("scheduleDate", existing.getScheduleDate());
                    errorResponse.put("transactionCode", existing.getTransactionCode());
                    
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
                }
            }
            
            Transaction savedTransaction = transactionService.createTransactionFromRequest(request);
            return ResponseEntity.ok(savedTransaction);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "TRANSACTION_CREATION_FAILED");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

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

    @GetMapping("/check-active-membership")
    public ResponseEntity<?> checkActiveMembership(@RequestParam Long userId) {
        try {
            List<Transaction> activeMemberships = 
                transactionRepository.findActiveMembershipsByUser(userId, LocalDateTime.now());
            
            if (!activeMemberships.isEmpty()) {
                Transaction transaction = activeMemberships.get(0);
                Map<String, Object> response = new HashMap<>();
                response.put("hasActiveMembership", true);
                response.put("membershipType", transaction.getMembershipType());
                response.put("paymentStatus", transaction.getPaymentStatus().toString());
                response.put("paymentDate", transaction.getPaymentDate());
                response.put("membershipActivatedDate", transaction.getMembershipActivatedDate());
                response.put("membershipExpiryDate", transaction.getMembershipExpiryDate());
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
            
            if (isPaid && transaction.getMembershipType() != null && transaction.getMembershipActivatedDate() == null) {
                LocalDateTime now = LocalDateTime.now();
                transaction.setMembershipActivatedDate(now);
                transaction.setMembershipExpiryDate(now.plusMonths(1));
                transactionRepository.save(transaction);
            }
            
            response.put("valid", isPaid);
            response.put("transaction", transaction);
            response.put("userName", transaction.getUser().getUsername());
            response.put("userEmail", transaction.getUserEmail());
            response.put("paymentStatus", transaction.getPaymentStatus().toString());
            response.put("totalAmount", transaction.getTotalAmount());
            response.put("paymentDate", transaction.getPaymentDate());
            response.put("sessionCompleted", transaction.getSessionCompleted());
            
            if (transaction.getClassName() != null) {
                response.put("type", "CLASS");
                response.put("className", transaction.getClassName());
                response.put("scheduleDay", transaction.getScheduleDay());
                response.put("scheduleTime", transaction.getScheduleTime());
                response.put("scheduleDate", transaction.getScheduleDate());
            } else if (transaction.getMembershipType() != null) {
                response.put("type", "MEMBERSHIP");
                response.put("membershipType", transaction.getMembershipType());
                response.put("membershipActivatedDate", transaction.getMembershipActivatedDate());
                response.put("membershipExpiryDate", transaction.getMembershipExpiryDate());
            }
            
            if (isPaid) {
                if (transaction.getSessionCompleted()) {
                    response.put("message", "✅ Valid - Session completed");
                } else if (transaction.getMembershipType() != null && transaction.getMembershipExpiryDate() != null) {
                    if (LocalDateTime.now().isAfter(transaction.getMembershipExpiryDate())) {
                        response.put("message", "⚠️ Membership expired");
                    } else {
                        response.put("message", "✅ Valid - Membership active");
                    }
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