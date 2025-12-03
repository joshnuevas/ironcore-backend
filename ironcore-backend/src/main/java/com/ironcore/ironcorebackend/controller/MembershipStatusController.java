package com.ironcore.ironcorebackend.controller;

import com.ironcore.ironcorebackend.entity.Membership;
import com.ironcore.ironcorebackend.entity.PaymentStatus;
import com.ironcore.ironcorebackend.repository.MembershipRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memberships")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class MembershipStatusController {

    private final MembershipRepository membershipRepository;

    public MembershipStatusController(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMembershipStatus(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        List<Membership> memberships = membershipRepository.findByUserId(userId);

        boolean hasActive = false;
        boolean hasPending = false;
        Membership selected = null;
        PaymentStatus selectedStatus = null;

        for (Membership m : memberships) {
            PaymentStatus status = m.getPaymentStatus();

            boolean timeActive =
                    m.getMembershipActivatedDate() != null &&
                    m.getMembershipExpiryDate() != null &&
                    m.getMembershipExpiryDate().isAfter(now);

            // 🔹 When is this membership *approved & active*?
            boolean isApprovedAndActive =
                    status == PaymentStatus.COMPLETED && timeActive;

            // 🔹 When is this membership *waiting for admin*?
            boolean isWaitingForAdmin =
                    status == PaymentStatus.PENDING ||
                    (status == PaymentStatus.COMPLETED && !timeActive);

            // ✅ ACTIVE wins
            if (isApprovedAndActive) {
                hasActive = true;
                selected = m;
                selectedStatus = status;
                break;
            }

            // ✅ Pending admin verification or payment pending
            if (!hasPending && isWaitingForAdmin) {
                hasPending = true;
                selected = m;
                selectedStatus = status;
            }
        }

        result.put("hasActiveMembership", hasActive);
        result.put("hasPendingMembership", hasPending);

        if (selected != null) {
            result.put("membershipType", selected.getMembershipType());
            result.put("membershipActivatedDate", selected.getMembershipActivatedDate());
            result.put("membershipExpiryDate", selected.getMembershipExpiryDate());
            result.put("transactionCode", selected.getTransactionCode());
            result.put("membershipStatus", selectedStatus != null ? selectedStatus.name() : null);
        }

        return ResponseEntity.ok(result);
    }

}
