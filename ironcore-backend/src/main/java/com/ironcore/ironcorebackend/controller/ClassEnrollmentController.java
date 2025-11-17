package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.service.ClassEnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/class-enrollments")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ClassEnrollmentController {

    private final ClassEnrollmentService classEnrollmentService;

    public ClassEnrollmentController(ClassEnrollmentService classEnrollmentService) {
        this.classEnrollmentService = classEnrollmentService;
    }

    @PostMapping
    public ResponseEntity<ClassEnrollment> createEnrollment(@RequestBody ClassEnrollment enrollment) {
        ClassEnrollment saved = classEnrollmentService.saveEnrollment(enrollment);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<ClassEnrollment>> getAllEnrollments() {
        return ResponseEntity.ok(classEnrollmentService.getAllEnrollments());
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<ClassEnrollment> getEnrollmentById(@PathVariable Long enrollmentId) {
        Optional<ClassEnrollment> enrollment = classEnrollmentService.getEnrollmentById(enrollmentId);
        return enrollment.map(ResponseEntity::ok)
                         .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClassEnrollment>> getEnrollmentsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(classEnrollmentService.getEnrollmentsByUser(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ClassEnrollment>> getEnrollmentsByPaymentStatus(@PathVariable PaymentStatus status) {
        return ResponseEntity.ok(classEnrollmentService.getEnrollmentsByPaymentStatus(status));
    }

    @PutMapping("/{enrollmentId}/complete-session")
    public ResponseEntity<Void> completeSession(@PathVariable Long enrollmentId) {
        classEnrollmentService.completeSession(enrollmentId);
        return ResponseEntity.ok().build();
    }

}
