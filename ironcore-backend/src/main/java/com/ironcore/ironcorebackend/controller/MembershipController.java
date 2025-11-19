package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.service.MembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/memberships")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    public ResponseEntity<Membership> createMembership(@RequestBody Membership membership) {
        Membership saved = membershipService.saveMembership(membership);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Membership>> getAllMemberships() {
        return ResponseEntity.ok(membershipService.getAllMemberships());
    }

    @GetMapping("/{membershipId}")
    public ResponseEntity<Membership> getMembershipById(@PathVariable Long membershipId) {
        Optional<Membership> membership = membershipService.getMembershipById(membershipId);
        return membership.map(ResponseEntity::ok)
                         .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{transactionCode}")
    public ResponseEntity<Membership> getMembershipByTransactionCode(@PathVariable String transactionCode) {
        Optional<Membership> membership = membershipService.getMembershipByTransactionCode(transactionCode);
        return membership.map(ResponseEntity::ok)
                         .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Membership>> getMembershipsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(membershipService.getMembershipsByUser(userId));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Membership>> getActiveMembershipsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(membershipService.getActiveMembershipsByUser(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Membership>> getMembershipsByPaymentStatus(@PathVariable PaymentStatus status) {
        return ResponseEntity.ok(membershipService.getMembershipsByPaymentStatus(status));
    }

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

        // Expiry based on membershipType
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