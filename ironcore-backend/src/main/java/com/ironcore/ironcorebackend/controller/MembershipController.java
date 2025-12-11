package com.ironcore.ironcorebackend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.service.MembershipService;

@RestController
@RequestMapping("/api/memberships")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /**
     * Create a new membership.
     */
    @PostMapping
    public ResponseEntity<Membership> createMembership(@RequestBody Membership membership) {
        Membership saved = membershipService.saveMembership(membership);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get all memberships.
     */
    @GetMapping
    public ResponseEntity<List<Membership>> getAllMemberships() {
        return ResponseEntity.ok(membershipService.getAllMemberships());
    }

    /**
     * Get membership by ID.
     */
    @GetMapping("/{membershipId}")
    public ResponseEntity<Membership> getMembershipById(@PathVariable Long membershipId) {
        Optional<Membership> membership = membershipService.getMembershipById(membershipId);
        return membership.map(ResponseEntity::ok)
                         .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get membership by transaction code.
     */
    @GetMapping("/code/{transactionCode}")
    public ResponseEntity<Membership> getMembershipByTransactionCode(@PathVariable String transactionCode) {
        Optional<Membership> membership = membershipService.getMembershipByTransactionCode(transactionCode);
        return membership.map(ResponseEntity::ok)
                         .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get memberships by user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Membership>> getMembershipsByUser(@PathVariable Long userId) {
        List<Membership> memberships = membershipService.getMembershipsByUser(userId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get active memberships by user.
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Membership>> getActiveMembershipsByUser(@PathVariable Long userId) {
        List<Membership> activeMemberships = membershipService.getActiveMembershipsByUser(userId);
        return ResponseEntity.ok(activeMemberships);
    }

    /**
     * Get memberships by payment status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Membership>> getMembershipsByPaymentStatus(@PathVariable PaymentStatus status) {
        List<Membership> memberships = membershipService.getMembershipsByPaymentStatus(status);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Approve and activate a membership based on transaction code.
     */
    @PutMapping("/approve/{transactionCode}")
    public ResponseEntity<?> approveMembership(@PathVariable String transactionCode) {
        Optional<Membership> membershipOpt = membershipService.getMembershipByTransactionCode(transactionCode);

        if (membershipOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Membership m = membershipOpt.get();

        if (m.getMembershipActivatedDate() != null) {
            return ResponseEntity.badRequest().body("Membership already activated.");
        }

        LocalDateTime now = LocalDateTime.now();
        m.setMembershipActivatedDate(now);

        // Apply expiry logic based on membership type
        switch (m.getMembershipType().toUpperCase()) {
            case "SILVER":
            case "GOLD":
            case "PLATINUM":
                m.setMembershipExpiryDate(now.plusMonths(1));
                break;
            case "SESSION":
                m.setMembershipExpiryDate(now.plusDays(1));
                break;
            default:
                m.setMembershipExpiryDate(now.plusMonths(1));
        }

        Membership updated = membershipService.saveMembership(m);
        return ResponseEntity.ok(updated);
    }
}
