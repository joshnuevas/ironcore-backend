package com.ironcore.ironcorebackend.controller;

import java.util.List;
import java.util.Optional;

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

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.service.ClassEnrollmentService;
import com.ironcore.ironcorebackend.service.ClassEnrollmentService.ScheduleConflictResponse;

@RestController
@RequestMapping("/api/class-enrollments")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ClassEnrollmentController {

    private final ClassEnrollmentService classEnrollmentService;

    public ClassEnrollmentController(ClassEnrollmentService classEnrollmentService) {
        this.classEnrollmentService = classEnrollmentService;
    }

    /**
     * Create a new class enrollment.
     */
    @PostMapping
    public ResponseEntity<ClassEnrollment> createEnrollment(@RequestBody ClassEnrollment enrollment) {
        ClassEnrollment saved = classEnrollmentService.saveEnrollment(enrollment);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get all enrollments.
     */
    @GetMapping
    public ResponseEntity<List<ClassEnrollment>> getAllEnrollments() {
        List<ClassEnrollment> enrollments = classEnrollmentService.getAllEnrollments();
        return ResponseEntity.ok(enrollments);
    }

    /**
     * Get a single enrollment by ID.
     */
    @GetMapping("/{enrollmentId}")
    public ResponseEntity<ClassEnrollment> getEnrollmentById(@PathVariable Long enrollmentId) {
        Optional<ClassEnrollment> enrollment = classEnrollmentService.getEnrollmentById(enrollmentId);
        return enrollment
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get all enrollments for a specific user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClassEnrollment>> getEnrollmentsByUser(@PathVariable Long userId) {
        List<ClassEnrollment> enrollments = classEnrollmentService.getEnrollmentsByUser(userId);
        return ResponseEntity.ok(enrollments);
    }

    /**
     * Mark a session as completed for an enrollment.
     */
    @PutMapping("/{enrollmentId}/complete-session")
    public ResponseEntity<Void> completeSession(@PathVariable Long enrollmentId) {
        classEnrollmentService.completeSession(enrollmentId);
        return ResponseEntity.ok().build();
    }

    // ================================
    // 🔍 Schedule conflict check
    // ================================
    /**
     * Check if a user has a schedule conflict for a given schedule.
     */
    @GetMapping("/check-conflict")
    public ResponseEntity<ScheduleConflictResponse> checkScheduleConflict(
            @RequestParam Long userId,
            @RequestParam Long scheduleId
    ) {
        ScheduleConflictResponse response =
                classEnrollmentService.checkScheduleConflict(userId, scheduleId);
        return ResponseEntity.ok(response);
    }
}
