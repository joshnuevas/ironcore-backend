package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.ClassEnrollment;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    Optional<ClassEnrollment> findByTransactionCode(String transactionCode);

    List<ClassEnrollment> findByUserId(Long userId);

    List<ClassEnrollment> findByPaymentStatus(PaymentStatus paymentStatus);

    @Transactional
    @Modifying
    @Query("UPDATE ClassEnrollment c SET c.sessionCompleted = true WHERE c.id = :enrollmentId")
    void markSessionCompleted(@Param("enrollmentId") Long enrollmentId);

}
