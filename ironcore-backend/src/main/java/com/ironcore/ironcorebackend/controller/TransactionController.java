package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.service.MembershipService;
import com.ironcore.ironcorebackend.service.TransactionService;
import com.ironcore.ironcorebackend.repository.ClassEnrollmentRepository;
import com.ironcore.ironcorebackend.repository.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
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
    private final MembershipService membershipService;

    public TransactionController(TransactionService transactionService,
                                ClassEnrollmentRepository classEnrollmentRepository,
                                MembershipRepository membershipRepository,
                                MembershipService membershipService) {
        this.transactionService = transactionService;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.membershipRepository = membershipRepository;
        this.membershipService = membershipService; 
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

    @GetMapping("/check-active-membership")
    public ResponseEntity<Map<String, Object>> checkActiveMembership(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Membership> activeOpt = transactionService.getActiveMembership(userId);

        if (activeOpt.isEmpty()) {
            result.put("hasActiveMembership", false);
        } else {
            Membership m = activeOpt.get();
            result.put("hasActiveMembership", true);
            result.put("membershipType", m.getMembershipType());
            result.put("membershipActivatedDate", m.getMembershipActivatedDate());
            result.put("membershipExpiryDate", m.getMembershipExpiryDate());
            result.put("transactionCode", m.getTransactionCode());
        }

        return ResponseEntity.ok(result);
    }

    public Optional<Membership> getActiveMembership(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        LocalDateTime now = LocalDateTime.now();
        List<Membership> activeMemberships = membershipRepository.findByUserIdAndExpiryDateAfterQuery(userId, now);
        return activeMemberships.stream().findFirst();
    }

    @GetMapping("/check-active-enrollment")
    public ResponseEntity<Boolean> checkActiveEnrollment(
            @RequestParam Long userId,
            @RequestParam Long classId
    ) {
        boolean hasActive = transactionService.hasActiveEnrollment(userId, classId);
        return ResponseEntity.ok(hasActive);
    }

    @GetMapping("/check/{transactionCode}")
    public ResponseEntity<Map<String, Object>> checkTransactionCode(
            @PathVariable String transactionCode
    ) {
        Map<String, Object> body = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        if (transactionCode == null || transactionCode.trim().isEmpty()) {
            body.put("valid", false);
            body.put("message", "Transaction code is required.");
            return ResponseEntity.ok(body); // 200 so frontend doesn't go to catch
        }

        String code = transactionCode.trim().toUpperCase();

        Optional<Transaction> txOpt = transactionService.getTransactionByCode(code);
        if (txOpt.isEmpty()) {
            body.put("valid", false);
            body.put("message", "Transaction code not found.");
            return ResponseEntity.ok(body);
        }

        Transaction tx = txOpt.get();

        // Common fields
        body.put("transaction", tx);
        body.put("userName", tx.getUser().getUsername());
        body.put("userEmail", tx.getUser().getEmail());
        body.put("totalAmount", tx.getTotalAmount());
        body.put("paymentStatus", tx.getPaymentStatus().name());
        body.put("paymentDate", tx.getPaymentDate());

        // Default message
        body.put("valid", false);
        body.put("message", "Invalid or expired access for this code.");

        // Check related membership
        Optional<Membership> membershipOpt = membershipRepository.findByTransactionId(tx.getId());
        if (membershipOpt.isPresent()) {
            Membership m = membershipOpt.get();
            body.put("type", "MEMBERSHIP");
            body.put("membershipType", m.getMembershipType());

            boolean paid = tx.getPaymentStatus() == PaymentStatus.COMPLETED;
            boolean activated = m.getMembershipActivatedDate() != null;
            boolean notExpired = m.getMembershipExpiryDate() != null
                    && m.getMembershipExpiryDate().isAfter(now);

            // ✅ 1) Payment is done, but membership not yet activated → ACTIVATE NOW
            if (paid && !activated) {
                LocalDateTime activatedNow = now;
                m.setMembershipActivatedDate(activatedNow);

                // Set expiry based on plan
                String type = m.getMembershipType() != null
                        ? m.getMembershipType().toUpperCase()
                        : "";

                switch (type) {
                    case "SESSION":
                        // 1-day pass
                        m.setMembershipExpiryDate(activatedNow.plusDays(1));
                        break;
                    case "SILVER":
                    case "GOLD":
                    case "PLATINUM":
                    default:
                        // default: 1 month
                        m.setMembershipExpiryDate(activatedNow.plusMonths(1));
                        break;
                }

                membershipRepository.save(m);

                body.put("membershipActivatedDate", m.getMembershipActivatedDate());
                body.put("membershipExpiryDate", m.getMembershipExpiryDate());
                body.put("valid", true);
                body.put("message", "Membership activated and access granted.");
                return ResponseEntity.ok(body);
            }

            // ✅ 2) Already activated and not expired → still valid
            if (paid && activated && notExpired) {
                body.put("membershipActivatedDate", m.getMembershipActivatedDate());
                body.put("membershipExpiryDate", m.getMembershipExpiryDate());
                body.put("valid", true);
                body.put("message", "Valid active membership.");
                return ResponseEntity.ok(body);
            }

            // ❌ 3) Other cases (unpaid / expired)
            body.put("membershipActivatedDate", m.getMembershipActivatedDate());
            body.put("membershipExpiryDate", m.getMembershipExpiryDate());

            if (!paid) {
                body.put("message", "Payment not completed for this membership.");
            } else if (!notExpired) {
                body.put("message", "Membership has expired.");
            } else {
                body.put("message", "Membership is not valid.");
            }

            return ResponseEntity.ok(body);
        }

        // Check related class enrollment
        Optional<ClassEnrollment> enrollmentOpt = classEnrollmentRepository.findByTransactionId(tx.getId());
        if (enrollmentOpt.isPresent()) {
            ClassEnrollment ce = enrollmentOpt.get();
            body.put("type", "CLASS");
            body.put("className", ce.getClassEntity() != null ? ce.getClassEntity().getName() : null);

            if (ce.getSchedule() != null) {
                body.put("scheduleDay", ce.getSchedule().getDay());
                body.put("scheduleTime", ce.getSchedule().getTimeSlot());
            }

            boolean paid = tx.getPaymentStatus() == PaymentStatus.COMPLETED;
            boolean notCompleted = !Boolean.TRUE.equals(ce.getSessionCompleted());

            if (paid && notCompleted) {
                body.put("valid", true);
                body.put("message", "Valid class enrollment.");
            } else if (!paid) {
                body.put("message", "Payment not completed for this class.");
            } else {
                body.put("message", "Class session already completed.");
            }

            return ResponseEntity.ok(body);
        }

        // No membership or class linked
        body.put("type", "UNKNOWN");
        body.put("message", "Transaction exists but is not linked to a membership or class.");
        return ResponseEntity.ok(body);
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