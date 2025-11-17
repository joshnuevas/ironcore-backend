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

    public List<Membership> getActiveMembershipsByUser(Long userId) {
        return membershipRepository.findActiveMembershipsByUser(userId, LocalDateTime.now());
    }

    public List<Membership> getMembershipsByPaymentStatus(PaymentStatus status) {
        return membershipRepository.findByPaymentStatus(status);
    }
}