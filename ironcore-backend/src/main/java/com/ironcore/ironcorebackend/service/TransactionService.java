package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final ScheduleRepository scheduleRepository;
    private final MembershipRepository membershipRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              ClassRepository classRepository,
                              ScheduleRepository scheduleRepository,
                              MembershipRepository membershipRepository,
                              ClassEnrollmentRepository classEnrollmentRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.scheduleRepository = scheduleRepository;
        this.membershipRepository = membershipRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    // Generate a unique transaction code
    private String generateTransactionCode(String typeCode) {
        String prefix = "IRC";
        String type = (typeCode != null && typeCode.length() >= 3)
                ? typeCode.substring(0, 3).toUpperCase()
                : "GEN";
        return prefix + "-" + type + "-" + generateRandomAlphanumeric(5);
    }

    private String generateRandomAlphanumeric(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }

    // Create a new transaction
    public Transaction createTransaction(TransactionRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setUserEmail(user.getEmail());
        transaction.setProcessingFee(request.getProcessingFee());
        transaction.setTotalAmount(request.getTotalAmount());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setPaymentStatus(PaymentStatus.valueOf(request.getPaymentStatus().toUpperCase()));
        transaction.setPaymentDate(LocalDateTime.now());

        // Handle Membership
        if (request.getMembershipType() != null) {
            transaction.setMembershipType(request.getMembershipType());
            transaction.setTransactionCode(generateTransactionCode(request.getMembershipType()));
            transaction.setMembershipActivatedDate(request.getMembershipActivatedDate());
            transaction.setMembershipExpiryDate(request.getMembershipExpiryDate());
        }

        // Handle Class Transaction
        if (request.getScheduleId() != null) {
            Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                    .orElseThrow(() -> new RuntimeException("Schedule not found"));
            ClassEntity classEntity = schedule.getClassEntity();

            transaction.setSchedule(schedule);
            transaction.setClassEntity(classEntity);
            transaction.setClassName(classEntity.getName());
            transaction.setScheduleDay(schedule.getDay());
            transaction.setScheduleTime(schedule.getTimeSlot());
            transaction.setScheduleDate(schedule.getDate().toString());
            transaction.setTransactionCode(generateTransactionCode(classEntity.getName()));
        }

        return transactionRepository.save(transaction);
    }

    // Update transaction status and create related records
    public Transaction updateTransactionStatus(Long transactionId, String status) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setPaymentStatus(PaymentStatus.valueOf(status.toUpperCase()));

        // Handle Membership Completion
        if (transaction.getMembershipType() != null && status.equalsIgnoreCase("COMPLETED")) {
            Membership membership = new Membership();
            membership.setUser(transaction.getUser());
            membership.setTransaction(transaction);
            membership.setMembershipType(transaction.getMembershipType());
            membership.setMembershipActivatedDate(LocalDateTime.now());
            membership.setMembershipExpiryDate(LocalDateTime.now().plusMonths(1));
            membership.setPaymentMethod(transaction.getPaymentMethod());
            membership.setPaymentStatus(PaymentStatus.COMPLETED);
            membership.setProcessingFee(transaction.getProcessingFee());
            membership.setTotalAmount(transaction.getTotalAmount());
            membership.setTransactionCode(transaction.getTransactionCode());
            membership.setPaymentDate(LocalDateTime.now());

            membershipRepository.save(membership);

            // Update Transaction with membership dates
            transaction.setMembershipActivatedDate(membership.getMembershipActivatedDate());
            transaction.setMembershipExpiryDate(membership.getMembershipExpiryDate());
        }

        // Handle Class Enrollment Completion
        if (transaction.getClassEntity() != null && transaction.getSchedule() != null
                && status.equalsIgnoreCase("COMPLETED")) {

            ClassEnrollment classEnrollment = new ClassEnrollment();
            classEnrollment.setUser(transaction.getUser());
            classEnrollment.setClassEntity(transaction.getClassEntity());
            classEnrollment.setSchedule(transaction.getSchedule());
            classEnrollment.setTransaction(transaction); // Important to avoid null constraint
            classEnrollment.setTransactionCode(transaction.getTransactionCode());
            classEnrollment.setClassName(transaction.getClassName());
            classEnrollment.setScheduleDay(transaction.getScheduleDay());
            classEnrollment.setScheduleTime(transaction.getScheduleTime());
            classEnrollment.setScheduleDate(transaction.getScheduleDate());
            classEnrollment.setPaymentStatus(PaymentStatus.COMPLETED);
            classEnrollment.setTotalAmount(transaction.getTotalAmount());
            classEnrollment.setProcessingFee(transaction.getProcessingFee());
            classEnrollment.setPaymentMethod(transaction.getPaymentMethod());
            classEnrollment.setPaymentDate(transaction.getPaymentDate());
            classEnrollment.setSessionCompleted(false); // default false

            classEnrollmentRepository.save(classEnrollment);
        }

        return transactionRepository.save(transaction);
    }

    // Fetch all transactions
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // Fetch transaction by code
    public Optional<Transaction> getTransactionByCode(String code) {
        return transactionRepository.findByTransactionCode(code);
    }

    // Fetch transactions by user
    public List<Transaction> getTransactionsByUser(Long userId) {
        return transactionRepository.findByUserId(userId);
    }

    // Check if user has active membership
    public boolean hasActiveMembership(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        LocalDateTime now = LocalDateTime.now();
        return getTransactionsByUser(userId).stream()
                .anyMatch(transaction ->
                        PaymentStatus.COMPLETED.equals(transaction.getPaymentStatus()) &&
                        transaction.getMembershipType() != null &&
                        !"SESSION".equalsIgnoreCase(transaction.getMembershipType()) &&
                        transaction.getMembershipExpiryDate() != null &&
                        transaction.getMembershipExpiryDate().isAfter(now)
                );
    }

    // Check if user has active enrollment in a class
    public boolean hasActiveEnrollment(Long userId, Long classId) {
        return classEnrollmentRepository.findByUserId(userId).stream()
                .anyMatch(enrollment ->
                        enrollment.getClassEntity().getId().equals(classId) &&
                        PaymentStatus.COMPLETED.equals(enrollment.getPaymentStatus())
                );
    }
}
