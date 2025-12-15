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

    // ================================
    // Dependencies
    // ================================
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

    // =====================================================
    //  🔹 BASIC CRUD
    // =====================================================
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody TransactionRequest request) {
        Transaction saved = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{transactionCode}")
    public ResponseEntity<Transaction> getTransactionByCode(@PathVariable String transactionCode) {
        return transactionService.getTransactionByCode(transactionCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // =====================================================
    //  🔹 USER TRANSACTION HISTORY (COMBINED MEMBERSHIP + CLASS)
    // =====================================================
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(@PathVariable Long userId) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsByUser(userId);

            List<Map<String, Object>> result = transactions.stream()
                    .map(this::mapTransactionDetails)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return error("Failed to fetch user transactions: " + e.getMessage());
        }
    }

    private Map<String, Object> mapTransactionDetails(Transaction tx) {
        Map<String, Object> map = new HashMap<>();

        map.put("id", tx.getId());
        map.put("transactionCode", tx.getTransactionCode());
        map.put("totalAmount", tx.getTotalAmount());
        map.put("processingFee", tx.getProcessingFee());
        map.put("paymentMethod", tx.getPaymentMethod());
        map.put("paymentStatus", tx.getPaymentStatus());
        map.put("paymentDate", tx.getPaymentDate());

        classEnrollmentRepository.findByTransactionId(tx.getId()).ifPresent(enrollment -> {
            map.put("className", enrollment.getClassEntity() != null ? enrollment.getClassEntity().getName() : null);
            map.put("classId", enrollment.getClassEntity() != null ? enrollment.getClassEntity().getId() : null);
            map.put("sessionCompleted", enrollment.getSessionCompleted());

            if (enrollment.getSchedule() != null) {
                map.put("scheduleId", enrollment.getSchedule().getId());
                map.put("scheduleDay", enrollment.getSchedule().getDay());
                map.put("scheduleDate", enrollment.getSchedule().getDate());
                map.put("scheduleTime", enrollment.getSchedule().getTimeSlot());
            }
        });

        membershipRepository.findByTransactionId(tx.getId()).ifPresent(m -> {
            map.put("membershipType", m.getMembershipType());
            map.put("membershipActivatedDate", m.getMembershipActivatedDate());
            map.put("membershipExpiryDate", m.getMembershipExpiryDate());
        });

        return map;
    }

    // =====================================================
    //  🔹 UPDATE TRANSACTION STATUS
    // =====================================================
    @PutMapping("/{transactionId}/status")
    public ResponseEntity<Transaction> updateTransactionStatus(
            @PathVariable Long transactionId,
            @RequestParam String status
    ) {
        try {
            Transaction updated = transactionService.updateTransactionStatus(transactionId, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // =====================================================
    //  🔹 CHECK ACTIVE MEMBERSHIP
    // =====================================================
    @GetMapping("/check-active-membership")
    public ResponseEntity<Map<String, Object>> checkActiveMembership(@RequestParam Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Membership> memberships = membershipRepository.findByUserId(userId);

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

            // Active membership wins immediately
            if (status == PaymentStatus.COMPLETED && timeActive) {
                hasActive = true;
                selected = m;
                selectedStatus = status;
                break;
            }

            // Pending membership fallback
            if (!hasPending && (status == null || status != PaymentStatus.COMPLETED)) {
                hasPending = true;
                selected = m;
                selectedStatus = status;
            }
        }

        Map<String, Object> result = new HashMap<>();
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

    // =====================================================
    //  🔹 CHECK ACTIVE ENROLLMENT
    // =====================================================
    @GetMapping("/check-active-enrollment")
    public ResponseEntity<Map<String, Object>> checkActiveEnrollment(
            @RequestParam Long userId,
            @RequestParam Long classId
    ) {
        boolean active = transactionService.hasActiveEnrollment(userId, classId);
        return ResponseEntity.ok(Map.of("hasActiveEnrollment", active));
    }

    // =====================================================
    //  🔹 CHECK TRANSACTION CODE (MEMBERSHIP / CLASS VALIDATION)
    // =====================================================
    @GetMapping("/check/{transactionCode}")
    public ResponseEntity<Map<String, Object>> checkTransactionCode(@PathVariable String transactionCode) {
        Map<String, Object> body = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        if (isBlank(transactionCode)) {
            return ok(body, false, "Transaction code is required.");
        }

        String code = transactionCode.trim().toUpperCase();
        Optional<Transaction> txOpt = transactionService.getTransactionByCode(code);

        if (txOpt.isEmpty()) {
            return ok(body, false, "Transaction code not found.");
        }

        Transaction tx = txOpt.get();
        populateTransactionInfo(body, tx);

        // Check membership first
        Optional<Membership> membershipOpt = membershipRepository.findByTransactionId(tx.getId());
        if (membershipOpt.isPresent()) {
            return handleMembershipCheck(body, membershipOpt.get(), tx, now);
        }

        // Check class enrollment
        Optional<ClassEnrollment> enrollmentOpt = classEnrollmentRepository.findByTransactionId(tx.getId());
        if (enrollmentOpt.isPresent()) {
            return handleClassCheck(body, enrollmentOpt.get(), tx);
        }

        body.put("type", "UNKNOWN");
        return ok(body, false, "Transaction exists but is not linked to a membership or class.");
    }

    // =====================================================
    //  🔹 TRANSACTION DETAILS
    // =====================================================
    @GetMapping("/{transactionId}/details")
    public ResponseEntity<?> getTransactionWithDetails(@PathVariable Long transactionId) {
        try {
            Optional<Transaction> txOpt = transactionService.getTransactionByCode(transactionId.toString());
            if (txOpt.isEmpty()) {
                return error("Transaction not found", HttpStatus.NOT_FOUND);
            }

            Transaction tx = txOpt.get();
            Map<String, Object> result = mapTransactionWithAllDetails(tx);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return error("Failed to fetch transaction details: " + e.getMessage());
        }
    }

    // =====================================================
    //  🔹 PRIVATE HELPERS
    // =====================================================
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private ResponseEntity<Map<String, Object>> ok(Map<String, Object> body, boolean valid, String msg) {
        body.put("valid", valid);
        body.put("message", msg);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> error(String msg) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", msg));
    }

    private ResponseEntity<Map<String, Object>> error(String msg, HttpStatus status) {
        return ResponseEntity.status(status).body(Map.of("message", msg));
    }

    private void populateTransactionInfo(Map<String, Object> body, Transaction tx) {
        body.put("transaction", tx);
        body.put("userName", tx.getUser().getUsername());
        body.put("userEmail", tx.getUser().getEmail());
        body.put("totalAmount", tx.getTotalAmount());
        body.put("paymentStatus", tx.getPaymentStatus().name());
        body.put("paymentDate", tx.getPaymentDate());
    }

    private ResponseEntity<Map<String, Object>> handleMembershipCheck(
            Map<String, Object> body,
            Membership m,
            Transaction tx,
            LocalDateTime now
    ) {
        body.put("type", "MEMBERSHIP");
        body.put("membershipType", m.getMembershipType());

        boolean paid = tx.getPaymentStatus() == PaymentStatus.COMPLETED;
        boolean activated = m.getMembershipActivatedDate() != null;
        boolean notExpired = m.getMembershipExpiryDate() != null &&
                m.getMembershipExpiryDate().isAfter(now);

        // Auto-activation if paid but not yet activated
        if (paid && !activated) {
            activateMembership(m, now);
            membershipRepository.save(m);

            body.put("membershipActivatedDate", m.getMembershipActivatedDate());
            body.put("membershipExpiryDate", m.getMembershipExpiryDate());

            return ok(body, true, "Membership activated and access granted.");
        }

        // Valid active membership
        if (paid && activated && notExpired) {
            body.put("membershipActivatedDate", m.getMembershipActivatedDate());
            body.put("membershipExpiryDate", m.getMembershipExpiryDate());
            return ok(body, true, "Valid active membership.");
        }

        // Membership invalid state
        body.put("membershipActivatedDate", m.getMembershipActivatedDate());
        body.put("membershipExpiryDate", m.getMembershipExpiryDate());

        if (!paid) return ok(body, false, "Payment not completed for this membership.");
        if (!notExpired) return ok(body, false, "Membership has expired.");

        return ok(body, false, "Membership is not valid.");
    }

    private void activateMembership(Membership m, LocalDateTime now) {
        m.setMembershipActivatedDate(now);

        String type = m.getMembershipType() != null
                ? m.getMembershipType().toUpperCase()
                : "";

        switch (type) {
            case "SESSION":
                m.setMembershipExpiryDate(now.plusDays(1));
                break;
            case "SILVER":
            case "GOLD":
            case "PLATINUM":
            default:
                m.setMembershipExpiryDate(now.plusMonths(1));
                break;
        }
    }

    private ResponseEntity<Map<String, Object>> handleClassCheck(
            Map<String, Object> body,
            ClassEnrollment ce,
            Transaction tx
    ) {
        body.put("type", "CLASS");
        body.put("className", ce.getClassEntity() != null ? ce.getClassEntity().getName() : null);

        if (ce.getSchedule() != null) {
            body.put("scheduleDay", ce.getSchedule().getDay());
            body.put("scheduleTime", ce.getSchedule().getTimeSlot());
        }

        boolean paid = tx.getPaymentStatus() == PaymentStatus.COMPLETED;
        boolean notCompleted = !Boolean.TRUE.equals(ce.getSessionCompleted());

        if (paid && notCompleted) {
            return ok(body, true, "Valid class enrollment.");
        }
        if (!paid) {
            return ok(body, false, "Payment not completed for this class.");
        }

        return ok(body, false, "Class session already completed.");
    }

    private Map<String, Object> mapTransactionWithAllDetails(Transaction tx) {
        Map<String, Object> result = new HashMap<>();

        result.put("id", tx.getId());
        result.put("transactionCode", tx.getTransactionCode());
        result.put("totalAmount", tx.getTotalAmount());
        result.put("processingFee", tx.getProcessingFee());
        result.put("paymentMethod", tx.getPaymentMethod());
        result.put("paymentStatus", tx.getPaymentStatus());
        result.put("paymentDate", tx.getPaymentDate());
        result.put("user", tx.getUser());

        classEnrollmentRepository.findByTransactionId(tx.getId()).ifPresent(ce -> {
            Map<String, Object> data = new HashMap<>();
            data.put("enrollmentId", ce.getId());
            data.put("className", ce.getClassEntity() != null ? ce.getClassEntity().getName() : null);
            data.put("classId", ce.getClassEntity() != null ? ce.getClassEntity().getId() : null);
            data.put("sessionCompleted", ce.getSessionCompleted());

            if (ce.getSchedule() != null) {
                data.put("scheduleId", ce.getSchedule().getId());
                data.put("scheduleDay", ce.getSchedule().getDay());
                data.put("scheduleDate", ce.getSchedule().getDate());
                data.put("scheduleTime", ce.getSchedule().getTimeSlot());
            }

            result.put("classEnrollment", data);
        });

        membershipRepository.findByTransactionId(tx.getId()).ifPresent(m -> {
            Map<String, Object> membershipData = new HashMap<>();
            membershipData.put("membershipId", m.getId());
            membershipData.put("membershipType", m.getMembershipType());
            membershipData.put("membershipActivatedDate", m.getMembershipActivatedDate());
            membershipData.put("membershipExpiryDate", m.getMembershipExpiryDate());
            result.put("membership", membershipData);
        });

        return result;
    }
}