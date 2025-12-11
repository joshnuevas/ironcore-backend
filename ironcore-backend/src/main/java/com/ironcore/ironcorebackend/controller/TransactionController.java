package com.ironcore.ironcorebackend.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.entity.Transaction;
import com.ironcore.ironcorebackend.repository.ClassEnrollmentRepository;
import com.ironcore.ironcorebackend.repository.MembershipRepository;
import com.ironcore.ironcorebackend.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TransactionController {

    private final TransactionService transactionService;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final MembershipRepository membershipRepository;

    public TransactionController(
            TransactionService transactionService,
            ClassEnrollmentRepository classEnrollmentRepository,
            MembershipRepository membershipRepository
    ) {
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

            List<Map<String, Object>> transactionDetails = transactions.stream()
                .map(transaction -> {
                    Map<String, Object> result = new HashMap<>();

                    result.put("id", transaction.getId());
                    result.put("transactionCode", transaction.getTransactionCode());
                    result.put("totalAmount", transaction.getTotalAmount());
                    result.put("processingFee", transaction.getProcessingFee());
                    result.put("paymentMethod", transaction.getPaymentMethod());
                    result.put("paymentStatus", transaction.getPaymentStatus());
                    result.put("paymentDate", transaction.getPaymentDate());

                    Optional<ClassEnrollment> enrollment = classEnrollmentRepository.findByTransactionId(transaction.getId());
                    if (enrollment.isPresent()) {
                        ClassEnrollment ce = enrollment.get();
                        result.put("className", ce.getClassEntity() != null ? ce.getClassEntity().getName() : null);
                        result.put("classId", ce.getClassEntity() != null ? ce.getClassEntity().getId() : null);
                        result.put("sessionCompleted", ce.getSessionCompleted());

                        if (ce.getSchedule() != null) {
                            result.put("scheduleId", ce.getSchedule().getId());
                            result.put("scheduleDay", ce.getSchedule().getDay());
                            result.put("scheduleDate", ce.getSchedule().getDate());
                            result.put("scheduleTime", ce.getSchedule().getTimeSlot());
                        }
                    }

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
        LocalDateTime now = LocalDateTime.now();

        // Get all memberships for this user
        java.util.List<Membership> memberships = membershipRepository.findByUserId(userId);

        boolean hasActive = false;
        boolean hasPending = false;
        Membership selected = null;
        PaymentStatus selectedStatus = null;

        for (Membership m : memberships) {
            PaymentStatus status = m.getPaymentStatus();

            boolean timeActive =
                    m.getMembershipActivatedDate() != null &&
                    m.getMembershipExpiryDate() != null &&
                    m.getMembershipExpiryDate().isAfter(now);

            // ✅ ACTIVE: COMPLETED + not expired
            if (status == PaymentStatus.COMPLETED && timeActive) {
                hasActive = true;
                selected = m;
                selectedStatus = status;
                // Active is the strongest case, we can stop here
                break;
            }

            // ✅ PENDING: ANYTHING that is NOT COMPLETED (including null)
            if (!hasPending && (status == null || status != PaymentStatus.COMPLETED)) {
                hasPending = true;
                selected = m;
                selectedStatus = status;
                // don't break; we still loop to see if there is an active one
            }
        }

        result.put("hasActiveMembership", hasActive);
        result.put("hasPendingMembership", hasPending);

        if (selected != null) {
            result.put("membershipType", selected.getMembershipType());
            result.put("membershipActivatedDate", selected.getMembershipActivatedDate());
            result.put("membershipExpiryDate", selected.getMembershipExpiryDate());
            result.put("transactionCode", selected.getTransactionCode());
            result.put("membershipStatus", selectedStatus != null ? selectedStatus.name() : null);
        }

        return ResponseEntity.ok(result);
    }

    // This helper is now optional; you can delete it if unused elsewhere
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
            return ResponseEntity.ok(body);
        }

        String code = transactionCode.trim().toUpperCase();

        Optional<Transaction> txOpt = transactionService.getTransactionByCode(code);
        if (txOpt.isEmpty()) {
            body.put("valid", false);
            body.put("message", "Transaction code not found.");
            return ResponseEntity.ok(body);
        }

        Transaction tx = txOpt.get();

        body.put("transaction", tx);
        body.put("userName", tx.getUser().getUsername());
        body.put("userEmail", tx.getUser().getEmail());
        body.put("totalAmount", tx.getTotalAmount());
        body.put("paymentStatus", tx.getPaymentStatus().name());
        body.put("paymentDate", tx.getPaymentDate());

        body.put("valid", false);
        body.put("message", "Invalid or expired access for this code.");

        Optional<Membership> membershipOpt = membershipRepository.findByTransactionId(tx.getId());
        if (membershipOpt.isPresent()) {
            Membership m = membershipOpt.get();
            body.put("type", "MEMBERSHIP");
            body.put("membershipType", m.getMembershipType());

            boolean paid = tx.getPaymentStatus() == PaymentStatus.COMPLETED;
            boolean activated = m.getMembershipActivatedDate() != null;
            boolean notExpired = m.getMembershipExpiryDate() != null
                    && m.getMembershipExpiryDate().isAfter(now);

            if (paid && !activated) {
                LocalDateTime activatedNow = now;
                m.setMembershipActivatedDate(activatedNow);

                String type = m.getMembershipType() != null
                        ? m.getMembershipType().toUpperCase()
                        : "";

                switch (type) {
                    case "SESSION":
                        m.setMembershipExpiryDate(activatedNow.plusDays(1));
                        break;
                    case "SILVER":
                    case "GOLD":
                    case "PLATINUM":
                    default:
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

            if (paid && activated && notExpired) {
                body.put("membershipActivatedDate", m.getMembershipActivatedDate());
                body.put("membershipExpiryDate", m.getMembershipExpiryDate());
                body.put("valid", true);
                body.put("message", "Valid active membership.");
                return ResponseEntity.ok(body);
            }

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

        body.put("type", "UNKNOWN");
        body.put("message", "Transaction exists but is not linked to a membership or class.");
        return ResponseEntity.ok(body);
    }

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

            result.put("id", transaction.getId());
            result.put("transactionCode", transaction.getTransactionCode());
            result.put("totalAmount", transaction.getTotalAmount());
            result.put("processingFee", transaction.getProcessingFee());
            result.put("paymentMethod", transaction.getPaymentMethod());
            result.put("paymentStatus", transaction.getPaymentStatus());
            result.put("paymentDate", transaction.getPaymentDate());
            result.put("user", transaction.getUser());

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
