package com.ironcore.ironcorebackend.service;

import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.repository.MembershipRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;

    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Membership saveMembership(Membership membership) {
        return membershipRepository.save(membership);
    }

    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    public Optional<Membership> getMembershipById(Long id) {
        return membershipRepository.findById(id);
    }

    public Optional<Membership> getMembershipByTransactionCode(String code) {
        return membershipRepository.findByTransactionCode(code);
    }

    public List<Membership> getMembershipsByUser(Long userId) {
        return membershipRepository.findByUserId(userId);
    }

    // FIXED: Get active memberships for a specific user
    public List<Membership> getActiveMembershipsByUser(Long userId) {
        if (userId == null) {
            // If no userId provided, get all active memberships (for admin)
            return membershipRepository.findActiveMemberships(LocalDateTime.now());
        }
        return membershipRepository.findActiveMembershipsByUser(userId, LocalDateTime.now());
    }

    // NEW: Get all active memberships (admin use)
    public List<Membership> getAllActiveMemberships() {
        return membershipRepository.findActiveMemberships(LocalDateTime.now());
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
        return membershipRepository.findByTransactionId(transactionId);
    }
}