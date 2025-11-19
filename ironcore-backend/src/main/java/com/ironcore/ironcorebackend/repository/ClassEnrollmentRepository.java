package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    // Use transaction relationship instead of direct field
    Optional<ClassEnrollment> findByTransaction_TransactionCode(String transactionCode);

    List<ClassEnrollment> findByUserId(Long userId);

    Optional<ClassEnrollment> findByTransactionId(Long transactionId);

    List<ClassEnrollment> findByUserIdAndClassEntityId(Long userId, Long classEntityId);

    // ✅ NEW: include schedule in duplicate check (user + class + schedule)
    List<ClassEnrollment> findByUserIdAndClassEntityIdAndScheduleId(
            Long userId,
            Long classEntityId,
            Long scheduleId
    );

    @Query("SELECT ce FROM ClassEnrollment ce WHERE ce.schedule.id = :scheduleId")
    List<ClassEnrollment> findByScheduleId(@Param("scheduleId") Long scheduleId);

    List<ClassEnrollment> findByUserIdAndSessionCompletedFalse(Long userId);

    // Find paid enrollments using transaction relationship
    @Query("""
        SELECT ce FROM ClassEnrollment ce
        WHERE ce.transaction.paymentStatus = com.ironcore.ironcorebackend.entity.PaymentStatus.COMPLETED
    """)
    List<ClassEnrollment> findPaidEnrollments();

    @Transactional
    @Modifying
    @Query("UPDATE ClassEnrollment c SET c.sessionCompleted = true WHERE c.id = :enrollmentId")
    void markSessionCompleted(@Param("enrollmentId") Long enrollmentId);

    @Query("""
        SELECT ce FROM ClassEnrollment ce
        WHERE ce.user.id = :userId
        AND ce.schedule.date = :date
        AND ce.schedule.timeSlot = :timeSlot
        AND ce.sessionCompleted = false
        AND ce.transaction.paymentStatus = com.ironcore.ironcorebackend.entity.PaymentStatus.COMPLETED
    """)
    List<ClassEnrollment> findConflictingSchedules(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("timeSlot") String timeSlot
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM ClassEnrollment ce WHERE ce.schedule.id = :scheduleId")
    void deleteByScheduleId(Long scheduleId);
}
