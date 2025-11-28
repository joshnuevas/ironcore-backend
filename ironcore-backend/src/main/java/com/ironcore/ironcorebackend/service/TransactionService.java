package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.dto.TransactionRequest;
import com.ironcore.ironcorebackend.entity.*;
import com.ironcore.ironcorebackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private static final String TRANSACTION_PREFIX = "IRC";

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final ScheduleRepository scheduleRepository;
    private final MembershipRepository membershipRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    // Thread-safe in-memory storage for transaction context
    private final Map<Long, TransactionContext> transactionContexts;

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
        this.transactionContexts = new ConcurrentHashMap<>();
    }

    // ==============================
    // CREATE TRANSACTION
    // ==============================
    public Transaction createTransaction(TransactionRequest request) {
        logger.info("=== CREATE TRANSACTION STARTED ===");
        logger.info("Request - UserId: {}, MembershipType: {}, ScheduleId: {}, PaymentStatus: {}",
                request.getUserId(), request.getMembershipType(), request.getScheduleId(), request.getPaymentStatus());

        logger.info("=== RECEIVED REQUEST ===");
        logger.info("ScheduleId: {}, ClassId: {}", request.getScheduleId(), request.getClassId());
        logger.info("PaymentStatus: {}", request.getPaymentStatus());

        // Validate request
        validateTransactionRequest(request);

        Long userId = Objects.requireNonNull(request.getUserId(), "User ID cannot be null after validation");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Transaction transaction = createTransactionEntity(request, user);
        logger.info("Payment Status: {}", transaction.getPaymentStatus());

        // Generate transaction code based on type
        String transactionCode = generateTransactionCodeBasedOnRequest(request);
        transaction.setTransactionCode(transactionCode);

        // Create transaction context
        TransactionContext context = createTransactionContext(request);

        // Handle transaction type
        if (request.getMembershipType() != null) {
            handleMembershipTransaction(transaction, request);
        } else if (request.getScheduleId() != null) {
            handleClassTransaction(transaction, request, context);
        } else {
            handleGenericTransaction(transaction);
        }

        // Save transaction FIRST to get an ID
        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction saved with ID: {}", savedTransaction.getId());

        // Store context using the saved transaction ID if not fully paid
        if (shouldStoreContext(transaction.getPaymentStatus())) {
            storeTransactionContext(savedTransaction.getId(), context);
        }

        logger.info("=== CREATE TRANSACTION COMPLETED ===");
        return savedTransaction;
    }

    private String generateTransactionCodeBasedOnRequest(TransactionRequest request) {
        if (request.getMembershipType() != null) {
            return generateTransactionCode(request.getMembershipType());
        } else if (request.getScheduleId() != null) {
            try {
                Long scheduleId = Objects.requireNonNull(
                        request.getScheduleId(),
                        "Schedule ID cannot be null when generating code"
                );
                Schedule schedule = scheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));
                ClassEntity classEntity = schedule.getClassEntity();
                return generateTransactionCode(classEntity.getName());
            } catch (RuntimeException e) {
                logger.warn("Failed to generate class-specific transaction code, using generic", e);
                return generateTransactionCode("CLASS");
            }
        } else {
            return generateTransactionCode("GEN");
        }
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transaction request cannot be null");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request.getPaymentStatus() == null || request.getPaymentStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment status cannot be null or empty");
        }
    }

    private Transaction createTransactionEntity(TransactionRequest request, User user) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);

        // Avoid unnecessary unboxing warnings
        Double processingFee = request.getProcessingFee();
        Double totalAmount = request.getTotalAmount();
        transaction.setProcessingFee(processingFee != null ? processingFee : 0.0);
        transaction.setTotalAmount(totalAmount != null ? totalAmount : 0.0);

        transaction.setPaymentMethod(
                request.getPaymentMethod() != null ? request.getPaymentMethod() : "UNKNOWN"
        );

        PaymentStatus paymentStatus = PaymentStatus.valueOf(request.getPaymentStatus().toUpperCase());
        transaction.setPaymentStatus(paymentStatus);
        transaction.setPaymentDate(LocalDateTime.now());

        // Infer transaction type
        if (request.getMembershipType() != null) {
            transaction.setTransactionType(TransactionType.MEMBERSHIP);
        } else if (request.getScheduleId() != null || request.getClassId() != null) {
            transaction.setTransactionType(TransactionType.CLASS);
        } else {
            transaction.setTransactionType(TransactionType.GENERIC);
        }

        return transaction;
    }

    private TransactionContext createTransactionContext(TransactionRequest request) {
        TransactionContext context = new TransactionContext();
        context.setMembershipType(request.getMembershipType());
        context.setScheduleId(request.getScheduleId());
        context.setClassId(request.getClassId());
        return context;
    }

    private boolean shouldStoreContext(PaymentStatus paymentStatus) {
        return paymentStatus != PaymentStatus.COMPLETED;
    }

    // ==============================
    // HANDLERS BY TYPE
    // ==============================

    private void handleMembershipTransaction(Transaction transaction,
                                             TransactionRequest request) {
        logger.info("✅ This is a MEMBERSHIP transaction");
        logger.info("Generated Transaction Code: {}", transaction.getTransactionCode());

        if (transaction.getPaymentStatus() == PaymentStatus.COMPLETED) {
            logger.info("🔄 Creating membership record...");
            createMembershipFromTransaction(transaction, request);
        } else {
            logger.info("⏸️ Payment not completed, membership will be created once completed.");
        }
    }

    private void handleClassTransaction(Transaction transaction,
                                        TransactionRequest request,
                                        TransactionContext context) {
        logger.info("✅ This is a CLASS transaction");
        logger.info("Generated Transaction Code: {}", transaction.getTransactionCode());

        try {
            Long scheduleId = Objects.requireNonNull(
                    request.getScheduleId(),
                    "Schedule ID cannot be null when handling class transaction"
            );

            Schedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));
            ClassEntity classEntity = schedule.getClassEntity();

            // Store class context
            context.setClassId(classEntity.getId());
            context.setClassName(classEntity.getName());

            if (transaction.getPaymentStatus() == PaymentStatus.COMPLETED) {
                logger.info("🔄 Creating class enrollment record...");
                createClassEnrollmentFromTransaction(transaction, schedule, classEntity);
            } else {
                logger.info("⏸️ Payment not completed, will create enrollment when status becomes COMPLETED");
            }
        } catch (RuntimeException e) {
            logger.error("Error handling class transaction", e);
            throw new RuntimeException("Failed to process class transaction: " + e.getMessage(), e);
        }
    }

    private void handleGenericTransaction(Transaction transaction) {
        logger.info("✅ This is a GENERIC transaction");
        logger.info("Generated Transaction Code: {}", transaction.getTransactionCode());
    }

    // ==============================
    // CONTEXT STORAGE
    // ==============================

    private void storeTransactionContext(Long transactionId, TransactionContext context) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null when storing context");
        }
        if (context == null) {
            throw new IllegalArgumentException("Transaction context cannot be null");
        }

        transactionContexts.put(transactionId, context);
        logger.info("💾 Stored transaction context for ID: {}", transactionId);
        logger.info("Context - MembershipType: {}, ScheduleId: {}, ClassId: {}",
                context.getMembershipType(), context.getScheduleId(), context.getClassId());
    }

    // ==============================
    // CODE GENERATION
    // ==============================

    private String generateTransactionCode(String typeCode) {
        try {
            String type = (typeCode != null && typeCode.length() >= 3)
                    ? typeCode.substring(0, 3).toUpperCase()
                    : "GEN";
            return TRANSACTION_PREFIX + "-" + type + "-" + generateRandomAlphanumeric(5);
        } catch (RuntimeException e) {
            logger.warn("Error generating transaction code, using fallback", e);
            return TRANSACTION_PREFIX + "-GEN-" + generateRandomAlphanumeric(5);
        }
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

    // ==============================
    // MEMBERSHIP CREATION
    // ==============================

    private void createMembershipFromTransaction(Transaction transaction, TransactionRequest request) {
        try {
            logger.info("=== CREATING MEMBERSHIP (PENDING) ===");

            Membership membership = new Membership();
            membership.setUser(transaction.getUser());
            membership.setTransaction(transaction);
            membership.setMembershipType(request.getMembershipType());

            // Pending: no dates yet
            membership.setMembershipActivatedDate(null);
            membership.setMembershipExpiryDate(null);
            membership.setTransactionCode(transaction.getTransactionCode());

            Membership savedMembership = membershipRepository.save(membership);
            logger.info("✅ Pending membership saved with ID: {}", savedMembership.getId());
        } catch (RuntimeException e) {
            logger.error("❌ Error creating membership for transaction: {}", transaction.getId(), e);
            throw new RuntimeException("Failed to create membership: " + e.getMessage(), e);
        }
    }

    // ==============================
    // CLASS ENROLLMENT CREATION
    // ==============================

    private void createClassEnrollmentFromTransaction(Transaction transaction,
                                                      Schedule schedule,
                                                      ClassEntity classEntity) {
        try {
            logger.info("=== CREATING CLASS ENROLLMENT ===");

            Long userId = transaction.getUser().getId();
            var date = schedule.getDate();
            String timeSlot = schedule.getTimeSlot();

            // 1. Check schedule conflicts
            logger.info("Checking for schedule conflicts for User {} on {} at {}",
                    userId, date, timeSlot);

            List<ClassEnrollment> conflicts = classEnrollmentRepository.findConflictingSchedules(
                    userId,
                    date,
                    timeSlot
            );

            if (!conflicts.isEmpty()) {
                logger.warn("❌ Schedule conflict detected. Cannot enroll user {}.", userId);
                throw new RuntimeException(
                        "Cannot enroll. You already booked another class on "
                                + date + " at " + timeSlot + "."
                );
            }

            // 2. Create enrollment
            ClassEnrollment enrollment =
                    new ClassEnrollment(transaction.getUser(), classEntity, schedule, transaction);
            enrollment.setSessionCompleted(false);

            logger.info("Enrollment Details - Class: {}, Schedule: {}, User: {}",
                    classEntity.getName(), schedule.getId(), transaction.getUser().getUsername());

            // 3. Update Schedule Enrollment Count (null-safe)
            Integer enrolledCountObj = schedule.getEnrolledCount();
            int currentEnrolledCount = (enrolledCountObj != null) ? enrolledCountObj : 0;

            schedule.setEnrolledCount(currentEnrolledCount + 1);
            scheduleRepository.save(schedule);

            logger.info("✅ Schedule enrolled count updated from {} to {}",
                    currentEnrolledCount, schedule.getEnrolledCount());

            // 4. Save enrollment
            ClassEnrollment savedEnrollment = classEnrollmentRepository.save(enrollment);
            logger.info("✅ Enrollment saved with ID: {}", savedEnrollment.getId());
            logger.info("=== ENROLLMENT CREATION COMPLETE ===");

        } catch (RuntimeException e) {
            logger.error("❌ Error creating class enrollment for transaction {}: {}",
                    transaction.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create class enrollment: " + e.getMessage(), e);
        }
    }

    // ==============================
    // STATUS UPDATE
    // ==============================

    public Transaction updateTransactionStatus(Long transactionId, String status) {
        logger.info("=== UPDATE TRANSACTION STATUS ===");
        logger.info("Transaction ID: {}, New Status: {}", transactionId, status);

        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));

        PaymentStatus newStatus = PaymentStatus.valueOf(status.toUpperCase());
        PaymentStatus oldStatus = transaction.getPaymentStatus();
        transaction.setPaymentStatus(newStatus);

        logger.info("Old Status: {}, New Status: {}", oldStatus, newStatus);

        // Handle domain record creation when payment completes
        if (newStatus == PaymentStatus.COMPLETED && oldStatus != PaymentStatus.COMPLETED) {
            logger.info("🔄 Payment completed - checking for domain records...");
            handleCompletedPayment(transaction);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("✅ Transaction status updated to: {}", savedTransaction.getPaymentStatus());
        logger.info("=== UPDATE TRANSACTION STATUS COMPLETE ===");
        return savedTransaction;
    }

    private void handleCompletedPayment(Transaction transaction) {
        boolean membershipExists = membershipRepository.findByTransactionId(transaction.getId()).isPresent();
        boolean enrollmentExists = classEnrollmentRepository.findByTransactionId(transaction.getId()).isPresent();

        logger.info("Membership exists: {}, Enrollment exists: {}", membershipExists, enrollmentExists);

        if (!membershipExists && !enrollmentExists) {
            logger.info("🔄 Creating domain records for completed transaction...");
            createDomainRecordsForCompletedTransaction(transaction);
        } else {
            logger.info("✅ Domain records already exist");
        }
    }

    private void createDomainRecordsForCompletedTransaction(Transaction transaction) {
        try {
            logger.info("🔄 Creating domain records for transaction: {}", transaction.getId());
            logger.info("Transaction Code: {}", transaction.getTransactionCode());

            // Get stored transaction context
            TransactionContext context = transactionContexts.get(transaction.getId());

            if (context != null) {
                logger.info("📋 Found transaction context - MembershipType: {}, ScheduleId: {}, ClassId: {}",
                        context.getMembershipType(), context.getScheduleId(), context.getClassId());

                if (context.getMembershipType() != null) {
                    logger.info("📋 Creating membership from stored context");
                    createMembershipFromStoredContext(transaction, context);
                } else if (context.getScheduleId() != null && context.getClassId() != null) {
                    logger.info("📋 Creating class enrollment from stored context");
                    createClassEnrollmentFromStoredContext(transaction, context);
                } else {
                    logger.warn("❓ Unknown transaction type in context");
                }
            } else {
                // Fallback: try to determine from transaction code
                logger.warn("⚠️ No stored context - trying to determine from transaction code");
                determineAndCreateDomainRecordsFromCode(transaction);
            }

        } catch (RuntimeException e) {
            logger.error("❌ Error creating domain records for transaction: {}", transaction.getId(), e);
            throw new RuntimeException("Failed to create domain records: " + e.getMessage(), e);
        }
    }

    private void createMembershipFromStoredContext(Transaction transaction, TransactionContext context) {
        try {
            logger.info("🔄 Creating membership from stored context (PENDING)...");

            Membership membership = new Membership();
            membership.setUser(transaction.getUser());
            membership.setTransaction(transaction);
            membership.setMembershipType(context.getMembershipType());

            membership.setMembershipActivatedDate(null);
            membership.setMembershipExpiryDate(null);
            membership.setTransactionCode(transaction.getTransactionCode());

            Membership savedMembership = membershipRepository.save(membership);
            logger.info("✅ Pending membership created with ID: {}, Type: {}",
                    savedMembership.getId(), savedMembership.getMembershipType());

            transactionContexts.remove(transaction.getId());
        } catch (RuntimeException e) {
            logger.error("❌ Error creating membership from context for transaction: {}", transaction.getId(), e);
            throw new RuntimeException("Failed to create membership from context: " + e.getMessage(), e);
        }
    }

    private void createClassEnrollmentFromStoredContext(Transaction transaction, TransactionContext context) {
        try {
            logger.info("🔄 Creating class enrollment from stored context...");
            logger.info("Context - ScheduleId: {}, ClassId: {}", context.getScheduleId(), context.getClassId());

            // Validate required data using Objects.requireNonNull so the analyzer knows it's non-null
            Long scheduleId = Objects.requireNonNull(
                    context.getScheduleId(),
                    "ScheduleId is null in context"
            );
            Long classId = Objects.requireNonNull(
                    context.getClassId(),
                    "ClassId is null in context"
            );

            Schedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));

            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found with ID: " + classId));

            logger.info("✅ Found schedule: {} and class: {}", schedule.getId(), classEntity.getName());

            Long userId = transaction.getUser().getId();
            var date = schedule.getDate();
            String timeSlot = schedule.getTimeSlot();

            // 1. Check for schedule conflicts
            logger.info("Checking for schedule conflicts for User {} on {} at {} (stored context)",
                    userId, date, timeSlot);

            List<ClassEnrollment> conflicts = classEnrollmentRepository.findConflictingSchedules(
                    userId,
                    date,
                    timeSlot
            );

            if (!conflicts.isEmpty()) {
                logger.warn("❌ Schedule conflict detected for user {} on {} at {} (stored context)",
                        userId, date, timeSlot);

                throw new RuntimeException(
                        "Cannot enroll. You already booked another class on "
                                + date + " at " + timeSlot + "."
                );
            }

            // 2. Check duplicate active enrollment for same class + schedule
            boolean existingEnrollmentSameSchedule = classEnrollmentRepository
                    .findByUserIdAndClassEntityIdAndScheduleId(
                            userId,
                            classId,
                            scheduleId
                    )
                    .stream()
                    .anyMatch(enrollment ->
                            enrollment.isPaid() &&
                                    !Boolean.TRUE.equals(enrollment.getSessionCompleted())
                    );

            if (existingEnrollmentSameSchedule) {
                logger.warn("⚠️ Active enrollment already exists for user {} in class {} and schedule {}",
                        userId, classId, scheduleId);
                return;
            }

            // 3. Create enrollment
            ClassEnrollment enrollment =
                    new ClassEnrollment(transaction.getUser(), classEntity, schedule, transaction);
            enrollment.setSessionCompleted(false);

            logger.info("💾 Saving class enrollment from stored context...");

            // 4. Update schedule enrolled count (null-safe)
            Integer enrolledCountObj = schedule.getEnrolledCount();
            int currentEnrolledCount = (enrolledCountObj != null) ? enrolledCountObj : 0;

            schedule.setEnrolledCount(currentEnrolledCount + 1);
            logger.info("📊 Updating schedule enrollment count from {} to {} (stored context)",
                    currentEnrolledCount, schedule.getEnrolledCount());

            scheduleRepository.save(schedule);

            ClassEnrollment savedEnrollment = classEnrollmentRepository.save(enrollment);
            logger.info("✅ Class enrollment created with ID: {}", savedEnrollment.getId());
            logger.info("✅ Schedule enrollment count updated to: {}", schedule.getEnrolledCount());

            // 5. Clean up stored context
            transactionContexts.remove(transaction.getId());
            logger.info("🧹 Cleaned up stored context for transaction: {}", transaction.getId());

        } catch (RuntimeException e) {
            logger.error("❌ CRITICAL ERROR creating class enrollment from context for transaction: {}",
                    transaction.getId(), e);
            throw new RuntimeException("Failed to create class enrollment: " + e.getMessage(), e);
        }
    }

    private void determineAndCreateDomainRecordsFromCode(Transaction transaction) {
        if (transaction.getTransactionCode() != null) {
            String code = transaction.getTransactionCode().toUpperCase();

            if (code.contains("SIL") || code.contains("GOL") || code.contains("PLA") || code.contains("MEM")) {
                logger.info("📋 This appears to be a membership transaction");
                createMembershipForCompletedTransaction(transaction);
            } else {
                logger.warn("⚠️ Class transactions need stored context - cannot create enrollment");
            }
        } else {
            logger.warn("❓ No transaction code - cannot determine transaction type");
        }
    }

    private void createMembershipForCompletedTransaction(Transaction transaction) {
        try {
            logger.info("🔄 Creating membership for completed transaction (PENDING)...");

            Membership membership = new Membership();
            membership.setUser(transaction.getUser());
            membership.setTransaction(transaction);

            String membershipType = determineMembershipType(transaction);
            membership.setMembershipType(membershipType);

            membership.setMembershipActivatedDate(null);
            membership.setMembershipExpiryDate(null);
            membership.setTransactionCode(transaction.getTransactionCode());

            Membership savedMembership = membershipRepository.save(membership);
            logger.info("✅ Pending membership created with ID: {}, Type: {}",
                    savedMembership.getId(), savedMembership.getMembershipType());

        } catch (RuntimeException e) {
            logger.error("❌ Error creating membership for transaction: {}", transaction.getId(), e);
            throw new RuntimeException("Failed to create membership: " + e.getMessage(), e);
        }
    }

    private String determineMembershipType(Transaction transaction) {
        if (transaction.getTransactionCode() != null) {
            String code = transaction.getTransactionCode().toUpperCase();
            if (code.contains("SIL")) return "SILVER";
            if (code.contains("GOL")) return "GOLD";
            if (code.contains("PLA")) return "PLATINUM";
            if (code.contains("MEM")) return "MEMBERSHIP";
        }
        return "BASIC";
    }

    // ==============================
    // TRANSACTION CONTEXT CLASS
    // ==============================

    private static class TransactionContext {
        private String membershipType;
        private Long scheduleId;
        private Long classId;
        private String className;

        public String getMembershipType() {
            return membershipType;
        }

        public void setMembershipType(String membershipType) {
            this.membershipType = membershipType;
        }

        public Long getScheduleId() {
            return scheduleId;
        }

        public void setScheduleId(Long scheduleId) {
            this.scheduleId = scheduleId;
        }

        public Long getClassId() {
            return classId;
        }

        public void setClassId(Long classId) {
            this.classId = classId;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        @Override
        public String toString() {
            return String.format(
                    "TransactionContext{membershipType='%s', scheduleId=%s, classId=%s, className='%s'}",
                    membershipType,
                    scheduleId,
                    classId,
                    className
            );
        }
    }

    // ==============================
    // PUBLIC API METHODS
    // ==============================

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction code cannot be null or empty");
        }
        return transactionRepository.findByTransactionCode(code);
    }

    public List<Transaction> getTransactionsByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return transactionRepository.findByUserId(userId);
    }

    public boolean hasActiveMembership(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        LocalDateTime now = LocalDateTime.now();
        List<Membership> activeMemberships = membershipRepository.findByUserIdAndExpiryDateAfterQuery(userId, now);
        return !activeMemberships.isEmpty();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveEnrollment(Long userId, Long classId) {
        if (userId == null || classId == null) {
            throw new IllegalArgumentException("User ID and Class ID cannot be null");
        }
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByUserIdAndClassEntityId(userId, classId);
        return enrollments.stream()
                .anyMatch(enrollment ->
                        enrollment.isPaid() &&
                                !Boolean.TRUE.equals(enrollment.getSessionCompleted())
                );
    }

    public Optional<Membership> getActiveMembership(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        LocalDateTime now = LocalDateTime.now();
        List<Membership> activeMemberships = membershipRepository.findByUserIdAndExpiryDateAfterQuery(userId, now);
        return activeMemberships.stream().findFirst();
    }

    public List<ClassEnrollment> getUserEnrollments(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return classEnrollmentRepository.findByUserId(userId);
    }

    public List<Transaction> getTransactionsByPaymentStatus(PaymentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Payment status cannot be null");
        }
        return transactionRepository.findByPaymentStatus(status);
    }

    public Membership createMembershipDirectly(User user, String membershipType, int monthsDuration) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (membershipType == null || membershipType.trim().isEmpty()) {
            throw new IllegalArgumentException("Membership type cannot be null or empty");
        }
        if (monthsDuration <= 0) {
            throw new IllegalArgumentException("Months duration must be positive");
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setTotalAmount(0.0);
        transaction.setProcessingFee(0.0);
        transaction.setPaymentMethod("MANUAL");
        transaction.setPaymentStatus(PaymentStatus.COMPLETED);
        transaction.setPaymentDate(LocalDateTime.now());
        transaction.setTransactionCode(generateTransactionCode(membershipType));

        Transaction savedTransaction = transactionRepository.save(transaction);

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setTransaction(savedTransaction);
        membership.setMembershipType(membershipType);
        membership.setMembershipActivatedDate(LocalDateTime.now());
        membership.setMembershipExpiryDate(LocalDateTime.now().plusMonths(monthsDuration));

        return membershipRepository.save(membership);
    }

    // ==============================
    // DEBUG ENDPOINTS (inside service)
    // ==============================

    @PostMapping("/debug-transaction")
    public ResponseEntity<?> debugTransaction(@RequestParam Long transactionId) {
        try {
            logger.info("=== DEBUG TRANSACTION ===");

            if (transactionId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Transaction ID cannot be null"));
            }

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            logger.info("Transaction: {}", transaction);
            logger.info("Transaction Code: {}", transaction.getTransactionCode());
            logger.info("Payment Status: {}", transaction.getPaymentStatus());

            TransactionContext context = transactionContexts.get(transactionId);
            logger.info("Stored Context: {}", context);

            boolean membershipExists = membershipRepository.findByTransactionId(transactionId).isPresent();
            boolean enrollmentExists = classEnrollmentRepository.findByTransactionId(transactionId).isPresent();

            logger.info("Membership exists: {}", membershipExists);
            logger.info("Enrollment exists: {}", enrollmentExists);

            return ResponseEntity.ok(Map.of(
                    "transaction", transaction,
                    "storedContext", context,
                    "membershipExists", membershipExists,
                    "enrollmentExists", enrollmentExists
            ));

        } catch (RuntimeException e) {
            logger.error("Error in debug transaction for ID: {}", transactionId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/debug-transaction-context/{transactionId}")
    public ResponseEntity<?> debugTransactionContext(@PathVariable long transactionId) {
        try {
            logger.info("=== DEBUG TRANSACTION CONTEXT ===");

            Long key = transactionId; // autobox for map key

            TransactionContext context = transactionContexts.get(key);
            logger.info("Stored Context for ID {}: {}", key, context);

            Transaction transaction = transactionRepository.findById(key)
                    .orElse(null);
            logger.info("Transaction: {}", transaction);

            String className = (context != null ? context.getClassName() : null);

            return ResponseEntity.ok(Map.of(
                    "transactionId", key,
                    "storedContext", context != null ? context.toString() : "NULL",
                    "className", className,
                    "transaction", transaction
            ));

        } catch (RuntimeException e) {
            logger.error("Error debugging transaction context: {}", transactionId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
