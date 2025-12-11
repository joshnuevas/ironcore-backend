package com.ironcore.ironcorebackend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.ClassEntity;
import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.ClassRepository;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.service.ClassEnrollmentService;
import com.ironcore.ironcorebackend.service.MembershipService;

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

    /**
     * Assign selected classes to a membership for a given user.
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assignClassesToMembership(@RequestBody Map<String, Object> request) {
        try {
            long userId = Long.parseLong(request.get("userId").toString());
            long membershipId = Long.parseLong(request.get("membershipId").toString());

            // Safe conversion of classIds to List<Integer>
            List<Integer> classIds = extractClassIds(request.get("classIds"));
            if (classIds == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid classIds format. Expected a list of IDs.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Membership membership = membershipService.getMembershipById(membershipId)
                    .orElseThrow(() -> new RuntimeException("Membership not found"));

            List<ClassEnrollment> classEnrollments = createClassEnrollments(classIds, user, membership);

            // Save all enrollments
            for (ClassEnrollment enrollment : classEnrollments) {
                classEnrollmentService.saveEnrollment(enrollment);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Classes assigned successfully");
            response.put("classCount", classEnrollments.size());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) { // same behavior: anything thrown ends as 400
            logger.error("Error assigning classes to membership", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Extract classIds from request object.
     * Returns null if the format is invalid (non-list), to preserve original behavior.
     */
    @SuppressWarnings("unchecked")
    private List<Integer> extractClassIds(Object classIdsObj) {
        if (classIdsObj instanceof List<?> rawList) {
            List<Integer> classIds = new ArrayList<>();
            for (Object o : rawList) {
                if (o != null) {
                    classIds.add(Integer.valueOf(o.toString()));
                }
            }
            return classIds;
        }
        // Format mismatch – handled by caller with a BAD_REQUEST response
        return null;
    }

    /**
     * Create class enrollments for each selected class.
     */
    private List<ClassEnrollment> createClassEnrollments(
            List<Integer> classIds,
            User user,
            Membership membership
    ) {
        List<ClassEnrollment> enrollments = new ArrayList<>();

        for (Integer classId : classIds) {
            ClassEntity classEntity = classRepository.findById(classId.longValue())
                    .orElseThrow(() -> new RuntimeException("Class not found: " + classId));

            ClassEnrollment classEnrollment =
                    new ClassEnrollment(user, classEntity, null, membership.getTransaction());
            classEnrollment.setSessionCompleted(false);

            enrollments.add(classEnrollment);
        }

        return enrollments;
    }
}
