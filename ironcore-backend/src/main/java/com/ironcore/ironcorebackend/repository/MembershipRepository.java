package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    // Custom query to find by transaction code via relationship
    @Query("SELECT m FROM Membership m WHERE m.transaction.transactionCode = :transactionCode")
    Optional<Membership> findByTransactionCode(@Param("transactionCode") String transactionCode);

    List<Membership> findByUserId(Long userId);

    // Custom query to find by payment status via relationship
    @Query("SELECT m FROM Membership m WHERE m.transaction.paymentStatus = :paymentStatus")
    List<Membership> findByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus);

    // FIXED: Use new field names
    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId " +
           "AND m.membershipActivatedDate IS NOT NULL " +
           "AND m.membershipExpiryDate > :currentDate")
    List<Membership> findActiveMembershipsByUser(
        @Param("userId") Long userId,
        @Param("currentDate") LocalDateTime currentDate
    );

    // ADD THESE METHODS:
    Optional<Membership> findByTransactionId(Long transactionId);
    
    // FIXED: Use new field name
    @Query("SELECT m FROM Membership m WHERE m.membershipExpiryDate > :now")
    List<Membership> findActiveMemberships(@Param("now") LocalDateTime now);
    
    // FIXED: Use new field name
    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.membershipExpiryDate > :date")
    List<Membership> findByUserIdAndExpiryDateAfterQuery(@Param("userId") Long userId, @Param("date") LocalDateTime date);
    
    // FIXED: Use new field name
    @Query("SELECT m FROM Membership m WHERE m.membershipExpiryDate > :now AND m.transaction.paymentStatus = com.ironcore.ironcorebackend.entity.PaymentStatus.COMPLETED")
    List<Membership> findActivePaidMemberships(@Param("now") LocalDateTime now);
}