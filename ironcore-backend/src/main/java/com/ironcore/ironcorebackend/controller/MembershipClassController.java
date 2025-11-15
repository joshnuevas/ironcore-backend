package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/membership-classes")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class MembershipClassController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassRepository classRepository;

    @PostMapping("/assign")
    public ResponseEntity<?> assignClassesToMembership(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long membershipTransactionId = Long.valueOf(request.get("membershipTransactionId").toString());
            List<Integer> classIds = (List<Integer>) request.get("classIds");

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Transaction membershipTransaction = transactionRepository.findById(membershipTransactionId)
                    .orElseThrow(() -> new RuntimeException("Membership transaction not found"));

            // Create class access transactions for each selected class
            List<Transaction> classTransactions = new ArrayList<>();

            for (Integer classId : classIds) {
                ClassEntity classEntity = classRepository.findById(Long.valueOf(classId))
                        .orElseThrow(() -> new RuntimeException("Class not found: " + classId));

                Transaction classTransaction = new Transaction();
                classTransaction.setUser(user);
                classTransaction.setUserEmail(user.getEmail());
                classTransaction.setClassEntity(classEntity);
                classTransaction.setClassName(classEntity.getName());
                classTransaction.setMembershipType(membershipTransaction.getMembershipType());
                classTransaction.setPaymentStatus(PaymentStatus.COMPLETED);
                classTransaction.setPaymentDate(LocalDateTime.now());
                classTransaction.setTotalAmount(0.0); // Included in membership
                classTransaction.setProcessingFee(0.0);
                classTransaction.setPaymentMethod("Membership");
                classTransaction.setTransactionCode(generateClassAccessCode(classEntity.getName()));

                // Set membership validity period
                classTransaction.setMembershipActivatedDate(membershipTransaction.getMembershipActivatedDate());
                classTransaction.setMembershipExpiryDate(membershipTransaction.getMembershipExpiryDate());

                classTransactions.add(classTransaction);
            }

            transactionRepository.saveAll(classTransactions);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Classes assigned successfully");
            response.put("classCount", classTransactions.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private String generateClassAccessCode(String className) {
        String prefix = "IRC";
        String type = className.length() >= 3 ? className.substring(0, 3).toUpperCase() : className.toUpperCase();
        String randomPart = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return String.format("%s-%s-%s", prefix, type, randomPart);
    }
}