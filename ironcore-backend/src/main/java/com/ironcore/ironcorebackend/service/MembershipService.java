package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.repository.MembershipRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;

    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Membership saveMembership(Membership membership) {
        // Ensure non-null for null-safety annotations
        return membershipRepository.save(Objects.requireNonNull(membership));
    }

    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    public Optional<Membership> getMembershipById(Long id) {
        // Make analyzer happy that id cannot be null
        return membershipRepository.findById(Objects.requireNonNull(id));
    }

    public Optional<Membership> getMembershipByTransactionCode(String code) {
        return membershipRepository.findByTransactionCode(code);
    }

    public List<Membership> getMembershipsByUser(Long userId) {
        return membershipRepository.findByUserId(Objects.requireNonNull(userId));
    }

    // FIXED: Single implementation of getActiveMembershipsByUser
    public List<Membership> getActiveMembershipsByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        LocalDateTime now = LocalDateTime.now();
        return membershipRepository.findActiveMembershipsByUser(userId, now)
                .stream()
                .filter(m -> m.getPaymentStatus() == PaymentStatus.COMPLETED)
                .filter(Membership::isCurrentlyActive)
                .collect(Collectors.toList());
    }

    // NEW: Get all active memberships (admin use)
    public List<Membership> getAllActiveMemberships() {
        return membershipRepository.findActiveMemberships(LocalDateTime.now())
                .stream()
                .filter(m -> m.getPaymentStatus() == PaymentStatus.COMPLETED)
                .filter(Membership::isCurrentlyActive)
                .collect(Collectors.toList());
    }

    public List<Membership> getMembershipsByPaymentStatus(PaymentStatus status) {
        return membershipRepository.findByPaymentStatus(status);
    }

    // NEW: Check if user has active membership
    public boolean hasActiveMembership(Long userId) {
        List<Membership> activeMemberships = getActiveMembershipsByUser(userId);
        return !activeMemberships.isEmpty();
    }

    // NEW: Get membership by transaction ID
    public Optional<Membership> getMembershipByTransactionId(Long transactionId) {
        return membershipRepository.findByTransactionId(Objects.requireNonNull(transactionId));
    }

    // NEW: Get all memberships by user (including expired)
    public List<Membership> getAllMembershipsByUser(Long userId) {
        return membershipRepository.findByUserId(Objects.requireNonNull(userId));
    }
}
