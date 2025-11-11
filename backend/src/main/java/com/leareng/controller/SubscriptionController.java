package com.leareng.controller;

import com.leareng.entity.Subscription;
import com.leareng.entity.SubscriptionRequest;
import com.leareng.entity.User;
import com.leareng.repository.UserRepository;
import com.leareng.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscription")
@CrossOrigin(origins = "*")
public class SubscriptionController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/request")
    public ResponseEntity<?> requestSubscription(Authentication authentication) {
        try {
            String email = authentication.getName();
            subscriptionService.requestSubscription(email);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "구독 신청이 완료되었습니다. 입금 확인 후 승인됩니다.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getSubscriptionStatus(Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            User user = userOpt.get();
            boolean isSubscribed = subscriptionService.checkSubscriptionStatus(user);
            LocalDateTime expiryDate = subscriptionService.getSubscriptionExpiry(user);
            int remainingQuestions = subscriptionService.getRemainingQuestions(user);
            String tier = subscriptionService.getCurrentTier(user);
            
            // 체험 여부 확인
            Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionByUser(user);
            boolean isTrial = false;
            if (subscriptionOpt.isPresent()) {
                Subscription sub = subscriptionOpt.get();
                isTrial = sub.getIsActive() && 
                         sub.getIsTrial() != null && 
                         sub.getIsTrial();
            }
            
            // 티어별 가격 정보
            int tierPrice = SubscriptionService.getTierPrice(tier);
            
            Map<String, Object> response = new HashMap<>();
            response.put("isSubscribed", isSubscribed);
            response.put("expiryDate", expiryDate);
            response.put("remainingQuestions", remainingQuestions);
            response.put("tier", tier);
            response.put("isTrial", isTrial);
            response.put("tierPrice", tierPrice);
            response.put("totalQuestionsSolved", user.getTotalQuestionsSolved());
            response.put("isAdmin", user.getIsAdmin());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "구독 상태 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingSubscriptions(Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty() || !userOpt.get().getIsAdmin()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "관리자 권한이 필요합니다.");
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(error);
            }
            
            List<SubscriptionRequest> requests = subscriptionService.getPendingSubscriptions();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "구독 요청 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/approve/{requestId}")
    public ResponseEntity<?> approveSubscription(@PathVariable Long requestId, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty() || !userOpt.get().getIsAdmin()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "관리자 권한이 필요합니다.");
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(error);
            }
            
            subscriptionService.approveSubscription(requestId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "구독 요청이 승인되었습니다.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/deny/{requestId}")
    public ResponseEntity<?> denySubscription(@PathVariable Long requestId, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty() || !userOpt.get().getIsAdmin()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "관리자 권한이 필요합니다.");
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(error);
            }
            
            subscriptionService.denySubscription(requestId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "구독 요청이 거절되었습니다.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeTier(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            User user = userOpt.get();
            
            // 요청에서 업그레이드할 티어 추출
            String newTier = null;
            if (request.get("tier") != null) {
                newTier = request.get("tier").toString();
            }
            
            if (newTier == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "티어 정보가 필요합니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 무료 체험은 BASIC 티어만 가능
            if (!"BASIC".equals(newTier)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "무료 체험은 BASIC 티어만 가능합니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 이미 유료 구독이 활성화되어 있고 체험이 아니면 체험 업그레이드 불가
            Optional<Subscription> subscriptionOpt = subscriptionService.getSubscriptionByUser(user);
            if (subscriptionOpt.isPresent()) {
                Subscription sub = subscriptionOpt.get();
                if (sub.getIsActive() && !sub.getIsTrial()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "이미 유료 구독이 활성화되어 있습니다.");
                    return ResponseEntity.badRequest().body(error);
                }
            }
            
            // 티어 업그레이드 실행 (무료 체험)
            subscriptionService.upgradeTier(user, newTier);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "티어가 업그레이드되었습니다.");
            response.put("tier", newTier);
            response.put("totalQuestionsSolved", user.getTotalQuestionsSolved());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "티어 업그레이드 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}


