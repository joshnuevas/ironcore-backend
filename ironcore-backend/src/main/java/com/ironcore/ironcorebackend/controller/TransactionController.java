package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.service.TransactionService;
import com.ironcore.ironcorebackend.repository.ClassEnrollmentRepository;
import com.ironcore.ironcorebackend.repository.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TransactionController {

    private final TransactionService transactionService;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final MembershipRepository membershipRepository;

    public TransactionController(TransactionService transactionService,
                                ClassEnrollmentRepository classEnrollmentRepository,
                                MembershipRepository membershipRepository) {
        this.transactionService = transactionService;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.membershipRepository = membershipRepository;
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

    // UPDATED: Returns combined data for frontend
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(@PathVariable Long userId) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsByUser(userId);
            
            // Transform transactions to include class and membership data
            List<Map<String, Object>> transactionDetails = transactions.stream()
                .map(transaction -> {
                    Map<String, Object> result = new HashMap<>();
                    
                    // Basic transaction data
                    result.put("id", transaction.getId());
                    result.put("transactionCode", transaction.getTransactionCode());
                    result.put("totalAmount", transaction.getTotalAmount());
                    result.put("processingFee", transaction.getProcessingFee());
                    result.put("paymentMethod", transaction.getPaymentMethod());
                    result.put("paymentStatus", transaction.getPaymentStatus());
                    result.put("paymentDate", transaction.getPaymentDate());
                    
                    // Check for ClassEnrollment
                    Optional<ClassEnrollment> enrollment = classEnrollmentRepository.findByTransactionId(transaction.getId());
                    if (enrollment.isPresent()) {
                        ClassEnrollment ce = enrollment.get();
                        result.put("className", ce.getClassEntity() != null ? ce.getClassEntity().getName() : null);
                        result.put("classId", ce.getClassEntity() != null ? ce.getClassEntity().getId() : null);
                        result.put("sessionCompleted", ce.getSessionCompleted());
                        
                        // Schedule data
                        if (ce.getSchedule() != null) {
                            result.put("scheduleId", ce.getSchedule().getId());
                            result.put("scheduleDay", ce.getSchedule().getDay());
                            result.put("scheduleDate", ce.getSchedule().getDate());
                            result.put("scheduleTime", ce.getSchedule().getTimeSlot());
                        }
                    }
                    
                    // Check for Membership
                    Optional<Membership> membership = membershipRepository.findByTransactionId(transaction.getId());
                    if (membership.isPresent()) {
                        Membership m = membership.get();
                        result.put("membershipType", m.getMembershipType());
                        result.put("membershipActivatedDate", m.getMembershipActivatedDate());
                        result.put("membershipExpiryDate", m.getMembershipExpiryDate());
                    }
                    
                    return result;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(transactionDetails);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch user transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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

    // NEW: Get transaction with full details by ID
    @GetMapping("/{transactionId}/details")
    public ResponseEntity<?> getTransactionWithDetails(@PathVariable Long transactionId) {
        try {
            Optional<Transaction> transactionOpt = transactionService.getTransactionByCode(transactionId.toString());
            if (transactionOpt.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Transaction not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            Transaction transaction = transactionOpt.get();
            Map<String, Object> result = new HashMap<>();
            
            // Basic transaction data
            result.put("id", transaction.getId());
            result.put("transactionCode", transaction.getTransactionCode());
            result.put("totalAmount", transaction.getTotalAmount());
            result.put("processingFee", transaction.getProcessingFee());
            result.put("paymentMethod", transaction.getPaymentMethod());
            result.put("paymentStatus", transaction.getPaymentStatus());
            result.put("paymentDate", transaction.getPaymentDate());
            result.put("user", transaction.getUser());
            
            // Check for ClassEnrollment
            Optional<ClassEnrollment> enrollment = classEnrollmentRepository.findByTransactionId(transaction.getId());
            if (enrollment.isPresent()) {
                ClassEnrollment ce = enrollment.get();
                Map<String, Object> enrollmentData = new HashMap<>();
                enrollmentData.put("enrollmentId", ce.getId());
                enrollmentData.put("className", ce.getClassEntity() != null ? ce.getClassEntity().getName() : null);
                enrollmentData.put("classId", ce.getClassEntity() != null ? ce.getClassEntity().getId() : null);
                enrollmentData.put("sessionCompleted", ce.getSessionCompleted());
                
                if (ce.getSchedule() != null) {
                    enrollmentData.put("scheduleId", ce.getSchedule().getId());
                    enrollmentData.put("scheduleDay", ce.getSchedule().getDay());
                    enrollmentData.put("scheduleDate", ce.getSchedule().getDate());
                    enrollmentData.put("scheduleTime", ce.getSchedule().getTimeSlot());
                }
                
                result.put("classEnrollment", enrollmentData);
            }
            
            // Check for Membership
            Optional<Membership> membership = membershipRepository.findByTransactionId(transaction.getId());
            if (membership.isPresent()) {
                Membership m = membership.get();
                Map<String, Object> membershipData = new HashMap<>();
                membershipData.put("membershipId", m.getId());
                membershipData.put("membershipType", m.getMembershipType());
                membershipData.put("membershipActivatedDate", m.getMembershipActivatedDate());
                membershipData.put("membershipExpiryDate", m.getMembershipExpiryDate());
                
                result.put("membership", membershipData);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to fetch transaction details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}