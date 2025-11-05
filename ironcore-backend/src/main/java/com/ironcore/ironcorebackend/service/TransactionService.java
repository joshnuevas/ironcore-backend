package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import org.springframework.stereotype.Service;
import com.ironcore.ironcorebackend.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

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

    // ⭐ ADD THIS: Generate unique transaction code
    private String generateTransactionCode(String className, String membershipType) {
        String prefix = "IRC";
        String type;
        
        // Determine type code (3 letters)
        if (membershipType != null) {
            // For memberships: SILVER → SIL, GOLD → GOL, PLATINUM → PLA
            type = membershipType.substring(0, 3).toUpperCase();
        } else if (className != null) {
            // For classes: HIIT → HII, ZUMBA → ZUM, SPIN → SPI, YOGA → YOG
            if (className.length() >= 3) {
                type = className.substring(0, 3).toUpperCase();
            } else {
                type = className.toUpperCase();
            }
        } else {
            type = "GEN"; // Generic fallback
        }
        
        // Generate 5 random alphanumeric characters
        String randomPart = generateRandomAlphanumeric(5);
        
        return String.format("%s-%s-%s", prefix, type, randomPart);
    }

    // ⭐ ADD THIS: Generate random alphanumeric string
    private String generateRandomAlphanumeric(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder result = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        
        return result.toString();
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

        String className = null;
        String membershipType = null;

        // Set class entity only if classId is provided (for class enrollments)
        if (request.getClassId() != null) {
            ClassEntity classEntity = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new RuntimeException("Class not found"));
            transaction.setClassEntity(classEntity);
            
            // Store class name
            className = classEntity.getName();
            transaction.setClassName(className);
        }

        // Set schedule only if scheduleId is provided (for class enrollments)
        if (request.getScheduleId() != null) {
            Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> new RuntimeException("Schedule not found"));
            transaction.setSchedule(schedule);
            
            // Store schedule details
            transaction.setScheduleDay(schedule.getDay());
            transaction.setScheduleTime(schedule.getTimeSlot());
            transaction.setScheduleDate(schedule.getDate().toString());
        }

        // Set membership type if provided
        if (request.getMembershipType() != null) {
            membershipType = request.getMembershipType();
            transaction.setMembershipType(membershipType);
        }

        // ⭐ ADD THIS: Generate and set transaction code
        String transactionCode = generateTransactionCode(className, membershipType);
        transaction.setTransactionCode(transactionCode);

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