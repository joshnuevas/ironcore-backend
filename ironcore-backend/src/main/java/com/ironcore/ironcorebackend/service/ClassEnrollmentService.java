package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.repository.ClassEnrollmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClassEnrollmentService {

    private final ClassEnrollmentRepository classEnrollmentRepository;

    public ClassEnrollmentService(ClassEnrollmentRepository classEnrollmentRepository) {
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    public ClassEnrollment saveEnrollment(ClassEnrollment enrollment) {
        return classEnrollmentRepository.save(enrollment);
    }

    public List<ClassEnrollment> getAllEnrollments() {
        return classEnrollmentRepository.findAll();
    }

    public Optional<ClassEnrollment> getEnrollmentById(Long id) {
        return classEnrollmentRepository.findById(id);
    }

    // UPDATED: Now uses the custom query
    public Optional<ClassEnrollment> getEnrollmentByTransactionCode(String code) {
        return classEnrollmentRepository.findByTransactionCode(code);
    }

    public List<ClassEnrollment> getEnrollmentsByUser(Long userId) {
        return classEnrollmentRepository.findByUserId(userId);
    }

    public void completeSession(Long enrollmentId) {
        classEnrollmentRepository.markSessionCompleted(enrollmentId);
    }
    
    // NEW: Get enrollments by schedule
    public List<ClassEnrollment> getEnrollmentsBySchedule(Long scheduleId) {
        return classEnrollmentRepository.findByScheduleId(scheduleId);
    }
    
    // NEW: Get paid enrollments
    public List<ClassEnrollment> getPaidEnrollments() {
        return classEnrollmentRepository.findPaidEnrollments();
    }
}