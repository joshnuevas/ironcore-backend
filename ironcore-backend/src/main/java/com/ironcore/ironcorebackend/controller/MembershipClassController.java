package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import com.ironcore.ironcorebackend.service.ClassEnrollmentService;
import com.ironcore.ironcorebackend.service.MembershipService;
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
    private MembershipService membershipService;

    @Autowired
    private ClassEnrollmentService classEnrollmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @PostMapping("/assign")
    public ResponseEntity<?> assignClassesToMembership(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long membershipId = Long.valueOf(request.get("membershipId").toString()); // Changed from membershipTransactionId
            List<Integer> classIds = (List<Integer>) request.get("classIds");

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the membership instead of transaction
            Membership membership = membershipService.getMembershipById(membershipId)
                    .orElseThrow(() -> new RuntimeException("Membership not found"));

            // Create class enrollments for each selected class
            List<ClassEnrollment> classEnrollments = new ArrayList<>();

            for (Integer classId : classIds) {
                ClassEntity classEntity = classRepository.findById(Long.valueOf(classId))
                        .orElseThrow(() -> new RuntimeException("Class not found: " + classId));

                ClassEnrollment classEnrollment = new ClassEnrollment(user, classEntity, null, membership.getTransaction());
                classEnrollment.setSessionCompleted(false);

                classEnrollments.add(classEnrollment);
            }

            // Save all enrollments
            for (ClassEnrollment enrollment : classEnrollments) {
                classEnrollmentService.saveEnrollment(enrollment);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Classes assigned successfully");
            response.put("classCount", classEnrollments.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}