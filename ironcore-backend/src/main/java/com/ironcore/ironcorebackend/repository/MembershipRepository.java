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

    Optional<Membership> findByTransactionCode(String transactionCode);

    List<Membership> findByUserId(Long userId);

    List<Membership> findByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId " +
           "AND m.membershipActivatedDate IS NOT NULL " +
           "AND m.membershipExpiryDate > :currentDate " +
           "ORDER BY m.paymentDate DESC")
    List<Membership> findActiveMembershipsByUser(
        @Param("userId") Long userId,
        @Param("currentDate") LocalDateTime currentDate
    );

}
