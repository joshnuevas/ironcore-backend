package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.ClassEntity;
import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.Schedule;
import com.ironcore.ironcorebackend.entity.Trainer;
import com.ironcore.ironcorebackend.entity.User;
import com.ironcore.ironcorebackend.repository.ClassEnrollmentRepository;
import com.ironcore.ironcorebackend.repository.ClassRepository;
import com.ironcore.ironcorebackend.repository.MembershipRepository;
import com.ironcore.ironcorebackend.repository.ScheduleRepository;
import com.ironcore.ironcorebackend.repository.TrainerRepository;
import com.ironcore.ironcorebackend.repository.UserRepository;
import com.ironcore.ironcorebackend.service.GeminiService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
public class AiController {

    private final GeminiService geminiService;
    private final ClassRepository classRepository;
    private final TrainerRepository trainerRepository;
    private final MembershipRepository membershipRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public AiController(
            GeminiService geminiService,
            ClassRepository classRepository,
            TrainerRepository trainerRepository,
            MembershipRepository membershipRepository,
            ScheduleRepository scheduleRepository,
            UserRepository userRepository,
            ClassEnrollmentRepository classEnrollmentRepository
    ) {
        this.geminiService = geminiService;
        this.classRepository = classRepository;
        this.trainerRepository = trainerRepository;
        this.membershipRepository = membershipRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    // Simple record to hold membership plan info (from your MembershipPage)
    private static record MembershipPlanInfo(
            String name,
            String price,
            String period,
            List<String> features
    ) {}

    // This matches your React MembershipPage plans exactly
    private static final List<MembershipPlanInfo> MEMBERSHIP_CATALOG = List.of(
            new MembershipPlanInfo(
                    "SESSION",
                    "₱200",
                    "/Day",
                    List.of(
                            "1 Day Gym Access",
                            "All Equipment Available",
                            "Locker Access",
                            "Perfect for Trial"
                    )
            ),
            new MembershipPlanInfo(
                    "SILVER",
                    "₱1,199",
                    "/Month",
                    List.of(
                            "Access to Gym Floor",
                            "Nutrition Plan",
                            "Locker Access",
                            "3 Classes"
                    )
            ),
            new MembershipPlanInfo(
                    "GOLD",
                    "₱1,699",
                    "/Month",
                    List.of(
                            "Access to Gym Floor",
                            "Nutrition & Fitness Plan",
                            "1 Session Trainer",
                            "5 Classes"
                    )
            ),
            new MembershipPlanInfo(
                    "PLATINUM",
                    "₱2,299",
                    "/Month",
                    List.of(
                            "Unlimited Classes",
                            "Access to Gym Floor",
                            "Nutrition & Fitness Plan",
                            "1 Session Trainer"
                    )
            )
    );

    @PostMapping("/chat")
    public Mono<Map<String, String>> chat(@RequestBody Map<String, Object> body) {
        // 1) Read message
        String message = Optional.ofNullable(body.get("message"))
                .map(Object::toString)
                .orElse("")
                .trim();

        // 2) Read optional userId (so AI can check *this* user's info)
        Long userId = null;
        Object userIdObj = body.get("userId");
        if (userIdObj instanceof Number num) {
            userId = num.longValue();
        } else if (userIdObj instanceof String s && !s.isBlank()) {
            try {
                // ✅ Fix: avoid "unnecessary temporary" by converting directly to Long
                userId = Long.valueOf(s.trim());
            } catch (NumberFormatException ignored) {}
        }

        // 3) Load global data from your DB
        List<ClassEntity> classes = classRepository.findAll();
        List<Trainer> trainers = trainerRepository.findAll();
        List<Membership> memberships = membershipRepository.findAll();
        List<Schedule> schedules = scheduleRepository.findAll();

        // 4) Load THIS user (if userId provided)
        User currentUser = null;
        if (userId != null) {
            currentUser = userRepository.findById(userId).orElse(null);
        }

        // 5) Load THIS user’s active memberships
        List<Membership> userActiveMemberships = Collections.emptyList();
        if (userId != null) {
            userActiveMemberships =
                    membershipRepository.findActiveMembershipsByUser(userId, LocalDateTime.now());
        }

        // 6) Load THIS user’s class enrollments (for advanced personalization)
        List<ClassEnrollment> userEnrollments = Collections.emptyList();
        if (userId != null) {
            userEnrollments = classEnrollmentRepository.findByUserId(userId);
        }

        // 7) Build a compact “knowledge base” text
        String gymDataText = buildGymDataText(
                currentUser,
                classes,
                trainers,
                memberships,
                schedules,
                userActiveMemberships,
                userEnrollments
        );

        // 8) Prompt: mix IronCore data + general fitness knowledge
        String prompt = """
                You are the virtual assistant of IronCore Gym, a real fitness center.
                You are answering questions for members inside the IronCore Gym web app.

                You have the following REAL data from the IronCore system:
                - The CURRENT USER (their name, email, and their membership + classes).
                - Membership PLANS (SESSION, SILVER, GOLD, PLATINUM) with prices and features.
                - CURRENT USER MEMBERSHIP status (if they have an active membership, its type and expiry).
                - CURRENT USER CLASS ENROLLMENTS (upcoming and recent past).
                - Global classes (names, descriptions, intensity, locations, prices, requirements, benefits).
                - Global trainers (names, specialties, locations, certifications, specializations, experience).
                - Global class schedules (class, day, date, time slot, available slots).

                CRITICAL RULES:
                - When the user asks about IronCore-specific things
                  (their membership status, membership plans, prices, classes, trainers, schedules),
                  you MUST use and respect the IronCore Gym data provided below.
                - Do NOT invent new membership types, prices, classes, trainers, or schedules that are
                  not in the IronCore data.
                - If the user asks things like "Do I have an active membership?",
                  "What plan am I currently on?", or "When does mine expire?",
                  use the CURRENT USER MEMBERSHIP section.
                - If there is no active membership for this user, clearly say
                  they do not currently have an active membership and may purchase one.

                - For general fitness, exercise, or basic nutrition questions
                  (for example: workout tips, simple nutrition guidance, stretching, recovery),
                  you MAY answer using your general fitness and nutrition knowledge.
                  Make sure the advice is safe, balanced, and not extreme.
                  You can optionally connect it to IronCore (e.g., which classes or memberships might help).

                - Only use the fallback
                  "I don't see that in the IronCore system. Please double-check in the app or at the front desk."
                  when the user asks for very specific account or system data that is clearly not present
                  (for example: exact payment reference numbers, promo codes, or transaction errors).

                - When you mention membership expiry dates, use a simple human readable format
                  like "December 21, 2025" and do NOT show time-of-day or milliseconds.

                - Keep answers short and structured: use bullet points or short paragraphs
                  so they are easy to read in the web app.
                - Do not give medical advice. If the question sounds medical or very personal,
                  suggest they consult a doctor or certified professional.

                ============= IRONCORE GYM DATA START =============
                %s
                ============= IRONCORE GYM DATA END ===============

                User question:
                %s
                """.formatted(gymDataText, message);

        return geminiService.generateText(prompt)
                .map(reply -> Map.of("reply", reply));
    }

    /**
     * Turn your user info, memberships, classes, trainers, schedules,
     * and user enrollments into a readable text that Gemini can use as a knowledge base.
     */
    private String buildGymDataText(
            User currentUser,
            List<ClassEntity> classes,
            List<Trainer> trainers,
            List<Membership> memberships,
            List<Schedule> schedules,
            List<Membership> userActiveMemberships,
            List<ClassEnrollment> userEnrollments
    ) {
        StringBuilder sb = new StringBuilder();

        // --- CURRENT USER INFO ---
        sb.append("CURRENT USER INFO:\n");
        if (currentUser == null) {
            sb.append("- No logged-in user information was provided.\n\n");
        } else {
            sb.append("- Username: ").append(nullSafe(currentUser.getUsername())).append("\n");
            sb.append("  Email: ").append(nullSafe(currentUser.getEmail())).append("\n\n");
        }

        // --- CURRENT USER MEMBERSHIP STATUS ---
        sb.append("CURRENT USER MEMBERSHIP STATUS:\n");
        if (userActiveMemberships == null || userActiveMemberships.isEmpty()) {
            sb.append("- No active membership found for this user right now.\n\n");
        } else {
            for (Membership m : userActiveMemberships) {
                sb.append("- Type: ").append(nullSafe(m.getMembershipType())).append("\n");
                sb.append("  Activated at: ")
                        .append(formatDateTime(m.getMembershipActivatedDate()))
                        .append("\n");
                sb.append("  Expires at: ")
                        .append(formatDateTime(m.getMembershipExpiryDate()))
                        .append("\n");
                sb.append("  Transaction code: ")
                        .append(nullSafe(m.getTransactionCode()))
                        .append("\n\n");
            }
        }

        // --- CURRENT USER CLASS ENROLLMENTS (UPCOMING + RECENT PAST) ---
        sb.append("CURRENT USER CLASS ENROLLMENTS:\n");
        if (userEnrollments == null || userEnrollments.isEmpty()) {
            sb.append("- This user has no recorded class enrollments.\n\n");
        } else {
            LocalDate today = LocalDate.now();

            List<ClassEnrollment> upcoming = userEnrollments.stream()
                    .filter(ce -> ce.getSchedule() != null && ce.getSchedule().getDate() != null)
                    .filter(ce -> !ce.getSchedule().getDate().isBefore(today))
                    .filter(ClassEnrollment::isPaid)
                    .filter(ce -> !Boolean.TRUE.equals(ce.getSessionCompleted()))
                    .sorted(Comparator
                            .comparing((ClassEnrollment ce) -> ce.getSchedule().getDate())
                            .thenComparing(ce -> ce.getSchedule().getTimeSlot()))
                    .collect(Collectors.toList());

            List<ClassEnrollment> past = userEnrollments.stream()
                    .filter(ce -> ce.getSchedule() != null && ce.getSchedule().getDate() != null)
                    .filter(ce -> ce.getSchedule().getDate().isBefore(today)
                            || Boolean.TRUE.equals(ce.getSessionCompleted()))
                    .sorted(Comparator
                            .comparing((ClassEnrollment ce) -> ce.getSchedule().getDate())
                            .reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            sb.append("UPCOMING CLASSES FOR THIS USER:\n");
            if (upcoming.isEmpty()) {
                sb.append("- (No upcoming classes for this user)\n");
            } else {
                for (ClassEnrollment ce : upcoming) {
                    ClassEntity c = ce.getClassEntity();
                    Schedule s = ce.getSchedule();
                    String className = (c != null ? c.getName() : "(unknown class)");
                    sb.append("- Class: ").append(nullSafe(className)).append("\n");
                    sb.append("  Date: ").append(nullSafe(s.getDate())).append("\n");
                    sb.append("  Time: ").append(nullSafe(s.getTimeSlot())).append("\n");
                    sb.append("  Transaction code: ").append(nullSafe(ce.getTransactionCode())).append("\n\n");
                }
            }

            sb.append("RECENT PAST CLASSES FOR THIS USER (last 5):\n");
            if (past.isEmpty()) {
                sb.append("- (No past classes recorded for this user)\n\n");
            } else {
                for (ClassEnrollment ce : past) {
                    ClassEntity c = ce.getClassEntity();
                    Schedule s = ce.getSchedule();
                    String className = (c != null ? c.getName() : "(unknown class)");
                    sb.append("- Class: ").append(nullSafe(className)).append("\n");
                    sb.append("  Date: ").append(nullSafe(s.getDate())).append("\n");
                    sb.append("  Time: ").append(nullSafe(s.getTimeSlot())).append("\n");
                    sb.append("  Completed: ").append(Boolean.TRUE.equals(ce.getSessionCompleted()) ? "Yes" : "No").append("\n\n");
                }
            }
        }

        // --- MEMBERSHIP PLANS (STATIC CATALOG FROM MEMBERSHIP PAGE) ---
        sb.append("MEMBERSHIP PLANS (CATALOG):\n");
        for (MembershipPlanInfo plan : MEMBERSHIP_CATALOG) {
            sb.append("- Name: ").append(plan.name()).append("\n");
            sb.append("  Price: ").append(plan.price())
                    .append(" ").append(plan.period()).append("\n");
            sb.append("  Features: ")
                    .append(String.join("; ", plan.features()))
                    .append("\n");

            long activeCount = memberships.stream()
                    .filter(m -> m.getMembershipType() != null)
                    .filter(m -> m.getMembershipType().equalsIgnoreCase(plan.name()))
                    .filter(Membership::isCurrentlyActive)
                    .count();
            if (activeCount > 0) {
                sb.append("  Currently active members (approx): ")
                        .append(activeCount)
                        .append("\n");
            }

            sb.append("\n");
        }

        // --- TRAINERS ---
        sb.append("TRAINERS:\n");
        if (trainers == null || trainers.isEmpty()) {
            sb.append("- (No trainers found in database)\n\n");
        } else {
            for (Trainer t : trainers) {
                sb.append("- Name: ").append(nullSafe(t.getName())).append("\n");
                sb.append("  Specialty: ").append(nullSafe(t.getSpecialty())).append("\n");
                sb.append("  Location: ").append(nullSafe(t.getLocation())).append("\n");
                if (t.getRating() > 0) {
                    sb.append("  Rating: ").append(t.getRating()).append("/5\n");
                }
                sb.append("  Experience: ").append(nullSafe(t.getExperience())).append("\n");
                sb.append("  Success rate: ").append(nullSafe(t.getSuccessRate())).append("\n");
                sb.append("  Sessions taught: ").append(nullSafe(t.getSessionsTaught())).append("\n");
                sb.append("  Availability: ").append(nullSafe(t.getAvailability())).append("\n");

                if (t.getCertifications() != null && !t.getCertifications().isEmpty()) {
                    sb.append("  Certifications: ")
                            .append(String.join(", ", t.getCertifications()))
                            .append("\n");
                }
                if (t.getSpecializations() != null && !t.getSpecializations().isEmpty()) {
                    sb.append("  Specializations: ")
                            .append(String.join(", ", t.getSpecializations()))
                            .append("\n");
                }
                sb.append("\n");
            }
        }

        // --- CLASSES ---
        sb.append("CLASSES:\n");
        if (classes == null || classes.isEmpty()) {
            sb.append("- (No classes found in database)\n\n");
        } else {
            for (ClassEntity c : classes) {
                sb.append("- Name: ").append(nullSafe(c.getName())).append("\n");
                sb.append("  Description: ").append(nullSafe(c.getDescription())).append("\n");
                sb.append("  Intensity: ").append(nullSafe(c.getIntensity())).append("\n");
                sb.append("  Duration: ").append(nullSafe(c.getDuration())).append("\n");
                sb.append("  Location: ").append(nullSafe(c.getLocation())).append("\n");
                sb.append("  Max participants: ").append(nullSafe(c.getMaxParticipants())).append("\n");
                sb.append("  Price: ₱").append(c.getPrice()).append("\n");
                sb.append("  Cancel policy: ").append(nullSafe(c.getCancelPolicy())).append("\n");

                if (c.getTrainer() != null) {
                    sb.append("  Trainer: ").append(nullSafe(c.getTrainer().getName())).append("\n");
                }

                if (c.getExpectations() != null && !c.getExpectations().isEmpty()) {
                    sb.append("  What to expect: ")
                            .append(String.join("; ", c.getExpectations()))
                            .append("\n");
                }
                if (c.getBenefits() != null && !c.getBenefits().isEmpty()) {
                    sb.append("  Benefits: ")
                            .append(String.join("; ", c.getBenefits()))
                            .append("\n");
                }
                if (c.getRequirements() != null && !c.getRequirements().isEmpty()) {
                    sb.append("  Requirements: ")
                            .append(String.join("; ", c.getRequirements()))
                            .append("\n");
                }

                sb.append("\n");
            }
        }

        // --- GLOBAL UPCOMING SCHEDULES ---
        sb.append("UPCOMING CLASS SCHEDULES (GLOBAL):\n");
        if (schedules == null || schedules.isEmpty()) {
            sb.append("- (No schedules found in database)\n\n");
        } else {
            LocalDate today = LocalDate.now();
            List<Schedule> upcoming = schedules.stream()
                    .filter(s -> s.getDate() != null && !s.getDate().isBefore(today))
                    .sorted(Comparator
                            .comparing(Schedule::getDate)
                            .thenComparing(Schedule::getTimeSlot))
                    .limit(40) // cap to avoid huge prompt
                    .collect(Collectors.toList());

            if (upcoming.isEmpty()) {
                sb.append("- (No upcoming schedules on or after today)\n\n");
            } else {
                for (Schedule s : upcoming) {
                    ClassEntity c = s.getClassEntity();
                    String className = (c != null ? c.getName() : "(unknown class)");

                    sb.append("- Class: ").append(nullSafe(className)).append("\n");
                    sb.append("  Date: ").append(nullSafe(s.getDate())).append("\n");
                    sb.append("  Day: ").append(nullSafe(s.getDay())).append("\n");
                    sb.append("  Time slot: ").append(nullSafe(s.getTimeSlot())).append("\n");
                    sb.append("  Max participants: ").append(nullSafe(s.getMaxParticipants())).append("\n");
                    sb.append("  Enrolled: ").append(nullSafe(s.getEnrolledCount())).append("\n");
                    sb.append("  Available slots: ").append(s.getAvailableSlots()).append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    private String nullSafe(Object value) {
        return value == null ? "N/A" : value.toString();
    }

    // Human-friendly date (no time, no milliseconds)
    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "N/A";
        return dt.toLocalDate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }
}
