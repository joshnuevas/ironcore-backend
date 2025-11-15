package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    // Generate unique transaction code
    private String generateTransactionCode(String className, String membershipType) {
        String prefix = "IRC";
        String type;

        if (membershipType != null) {
            type = membershipType.substring(0, 3).toUpperCase(); // e.g. SIL, GOL, PLA
        } else if (className != null) {
            type = className.length() >= 3 ? className.substring(0, 3).toUpperCase() : className.toUpperCase();
        } else {
            type = "GEN";
        }

        String randomPart = generateRandomAlphanumeric(5);
        return String.format("%s-%s-%s", prefix, type, randomPart);
    }

    // Generate random alphanumeric string
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
        // Fetch user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setUserEmail(user.getEmail());

        String className = null;
        String membershipType = null;

        // ✅ Handle class enrollment
        if (request.getScheduleId() != null) {
            Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> new RuntimeException("Schedule not found"));

            // Get class from the schedule
            ClassEntity classEntity = schedule.getClassEntity();

            // ⭐ REMOVED: Do NOT check if full here - allow pending transactions
            // ⭐ REMOVED: Do NOT increment enrolledCount here - only on payment completion

            // Link everything
            transaction.setSchedule(schedule);
            transaction.setClassEntity(classEntity);
            transaction.setClassName(classEntity.getName());
            transaction.setScheduleDay(schedule.getDay());
            transaction.setScheduleTime(schedule.getTimeSlot());
            transaction.setScheduleDate(schedule.getDate().toString());

            className = classEntity.getName();
        }

        // ✅ Handle membership transactions
        if (request.getMembershipType() != null) {
            membershipType = request.getMembershipType();
            transaction.setMembershipType(membershipType);
        }

        // Generate transaction code
        String transactionCode = generateTransactionCode(className, membershipType);
        transaction.setTransactionCode(transactionCode);

        // Payment details
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