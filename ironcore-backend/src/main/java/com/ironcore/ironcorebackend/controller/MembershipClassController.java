package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.ClassEntity;
import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.ClassRepository;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.service.ClassEnrollmentService;
import com.ironcore.ironcorebackend.service.MembershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/membership-classes")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class MembershipClassController {

    private static final Logger logger = LoggerFactory.getLogger(MembershipClassController.class);

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private ClassEnrollmentService classEnrollmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassRepository classRepository;

    @PostMapping("/assign")
    public ResponseEntity<?> assignClassesToMembership(@RequestBody Map<String, Object> request) {
        try {
            // ✅ Use primitives to avoid @NonNull Long hint
            long userId = Long.parseLong(request.get("userId").toString());
            long membershipId = Long.parseLong(request.get("membershipId").toString());

            // ✅ Safe conversion of classIds to List<Integer> (no unchecked cast)
            Object classIdsObj = request.get("classIds");
            List<Integer> classIds = new ArrayList<>();
            if (classIdsObj instanceof List<?> rawList) {
                for (Object o : rawList) {
                    if (o != null) {
                        classIds.add(Integer.valueOf(o.toString()));
                    }
                }
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid classIds format. Expected a list of IDs.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the membership instead of transaction
            Membership membership = membershipService.getMembershipById(membershipId)
                    .orElseThrow(() -> new RuntimeException("Membership not found"));

            // Create class enrollments for each selected class
            List<ClassEnrollment> classEnrollments = new ArrayList<>();

            for (Integer classId : classIds) {
                ClassEntity classEntity = classRepository.findById(classId.longValue())
                        .orElseThrow(() -> new RuntimeException("Class not found: " + classId));

                ClassEnrollment classEnrollment =
                        new ClassEnrollment(user, classEntity, null, membership.getTransaction());
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

        } catch (RuntimeException e) { // ✅ narrower than generic Exception
            logger.error("Error assigning classes to membership", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}
