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

    // Find membership by transaction code (via relationship)
    @Query("SELECT m FROM Membership m WHERE m.transaction.transactionCode = :transactionCode")
    Optional<Membership> findByTransactionCode(@Param("transactionCode") String transactionCode);

    // All memberships for a user
    List<Membership> findByUserId(Long userId);

    // By payment status via transaction
    @Query("SELECT m FROM Membership m WHERE m.transaction.paymentStatus = :paymentStatus")
    List<Membership> findByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus);

    // Active memberships by user (time-based only)
    @Query("SELECT m FROM Membership m " +
           "WHERE m.user.id = :userId " +
           "AND m.membershipActivatedDate IS NOT NULL " +
           "AND m.membershipExpiryDate > :currentDate")
    List<Membership> findActiveMembershipsByUser(
        @Param("userId") Long userId,
        @Param("currentDate") LocalDateTime currentDate
    );

    // Find membership linked to a specific transaction
    Optional<Membership> findByTransactionId(Long transactionId);

    // All memberships that have not yet expired (any user)
    @Query("SELECT m FROM Membership m WHERE m.membershipExpiryDate > :now")
    List<Membership> findActiveMemberships(@Param("now") LocalDateTime now);

    // All memberships for a user that are still valid (time-based)
    @Query("SELECT m FROM Membership m " +
           "WHERE m.user.id = :userId " +
           "AND m.membershipExpiryDate > :date")
    List<Membership> findByUserIdAndExpiryDateAfterQuery(
        @Param("userId") Long userId,
        @Param("date") LocalDateTime date
    );

    // All active, paid memberships (any user)
    @Query("SELECT m FROM Membership m " +
           "WHERE m.membershipExpiryDate > :now " +
           "AND m.transaction.paymentStatus = com.ironcore.ironcorebackend.entity.PaymentStatus.COMPLETED")
    List<Membership> findActivePaidMemberships(@Param("now") LocalDateTime now);

    // ✅ Single active, paid membership for a specific user
    @Query("SELECT m FROM Membership m " +
           "WHERE m.user.id = :userId " +
           "AND m.membershipExpiryDate > :now " +
           "AND m.transaction.paymentStatus = com.ironcore.ironcorebackend.entity.PaymentStatus.COMPLETED")
    Optional<Membership> findActivePaidMembershipByUser(
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now
    );

    // ✅ Single pending membership for a specific user (waiting for admin)
    @Query("SELECT m FROM Membership m " +
           "WHERE m.user.id = :userId " +
           "AND m.transaction.paymentStatus = com.ironcore.ironcorebackend.entity.PaymentStatus.PENDING")
    Optional<Membership> findPendingMembershipByUser(
        @Param("userId") Long userId
    );
}
