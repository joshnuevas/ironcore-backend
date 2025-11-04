package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import org.springframework.stereotype.Service;
import com.ironcore.ironcorebackend.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final ScheduleRepository scheduleRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            ClassRepository classRepository,
            ScheduleRepository scheduleRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public Transaction createTransactionFromRequest(TransactionRequest request) {
        // Fetch user (required for all transactions)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create the transaction
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        
        // Store user email
        transaction.setUserEmail(user.getEmail());

        // Set class entity only if classId is provided (for class enrollments)
        if (request.getClassId() != null) {
            ClassEntity classEntity = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new RuntimeException("Class not found"));
            transaction.setClassEntity(classEntity);
            
            // Store class name
            transaction.setClassName(classEntity.getName());
        }

        // Set schedule only if scheduleId is provided (for class enrollments)
        if (request.getScheduleId() != null) {
            Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> new RuntimeException("Schedule not found"));
            transaction.setSchedule(schedule);
            
            // ⭐ FIXED: Convert LocalDate to String
            transaction.setScheduleDay(schedule.getDay());
            transaction.setScheduleTime(schedule.getTimeSlot());
            
            // Option 1: Simple toString() - gives "2025-11-04" format
            transaction.setScheduleDate(schedule.getDate().toString());
            
            // Option 2: Custom format - gives "Nov 04, 2025" format (uncomment if preferred)
            // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            // transaction.setScheduleDate(schedule.getDate().format(formatter));
        }

        // Set membership type if provided
        if (request.getMembershipType() != null) {
            transaction.setMembershipType(request.getMembershipType());
        }

        // Set common transaction fields
        transaction.setProcessingFee(request.getProcessingFee());
        transaction.setTotalAmount(request.getTotalAmount());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setPaymentStatus(PaymentStatus.valueOf(request.getPaymentStatus()));
        transaction.setPaymentDate(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}