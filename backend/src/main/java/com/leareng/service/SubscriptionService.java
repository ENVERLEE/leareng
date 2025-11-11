package com.leareng.service;

import com.leareng.entity.Subscription;
import com.leareng.entity.SubscriptionRequest;
import com.leareng.entity.User;
import com.leareng.repository.SubscriptionRepository;
import com.leareng.repository.SubscriptionRequestRepository;
import com.leareng.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionService {
    
    // 일일 문제 풀이 한도
    private static final int FREE_QUESTION_LIMIT = 50;
    private static final int BASIC_QUESTION_LIMIT = 500;
    private static final int PREMIUM_QUESTION_LIMIT = 1000;
    
    // 티어 업그레이드 기준
    private static final int BASIC_THRESHOLD = 50;
    private static final int PREMIUM_THRESHOLD = 500;
    private static final int ACADEMY_THRESHOLD = 1000;
    
    // 무료 체험 기간 (일)
    private static final int TRIAL_PERIOD_DAYS = 3;
    
    // 티어별 가격 (원/월)
    public static final int PRICE_FREE = 0;
    public static final int PRICE_BASIC = 9900;
    public static final int PRICE_PREMIUM = 19900;
    public static final int PRICE_ACADEMY = 49900;
    
    /**
     * 티어별 가격 반환
     */
    public static int getTierPrice(String tier) {
        switch (tier) {
            case "BASIC":
                return PRICE_BASIC;
            case "PREMIUM":
                return PRICE_PREMIUM;
            case "ACADEMY":
                return PRICE_ACADEMY;
            case "FREE":
            default:
                return PRICE_FREE;
        }
    }
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @Autowired
    private SubscriptionRequestRepository subscriptionRequestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    public boolean checkSubscriptionStatus(User user) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        if (subscriptionOpt.isEmpty()) {
            return false;
        }
        
        Subscription subscription = subscriptionOpt.get();
        if (!subscription.getIsActive()) {
            return false;
        }
        
        // Check expiry
        if (subscription.getExpiryDate() != null && 
            subscription.getExpiryDate().isBefore(LocalDateTime.now())) {
            unsubscribe(user);
            return false;
        }
        
        return true;
    }
    
    public LocalDateTime getSubscriptionExpiry(User user) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().getIsActive()) {
            return null;
        }
        return subscriptionOpt.get().getExpiryDate();
    }
    
    /**
     * 일일 문제 풀이 한도 리셋 (필요한 경우)
     */
    public void resetDailyQuestionsIfNeeded(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastReset = user.getLastQuestionReset();
        
        if (lastReset == null || now.toLocalDate().isAfter(lastReset.toLocalDate())) {
            user.setQuestionsUsed(0);
            user.setLastQuestionReset(now);
            userRepository.save(user);
        }
    }
    
    public String getCurrentTier(User user) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().getIsActive()) {
            return "FREE";
        }
        return subscriptionOpt.get().getTier();
    }
    
    public Optional<Subscription> getSubscriptionByUser(User user) {
        return subscriptionRepository.findByUser(user);
    }
    
    public int getTierLimit(String tier) {
        switch (tier) {
            case "ACADEMY":
                return Integer.MAX_VALUE; // 무제한
            case "PREMIUM":
                return PREMIUM_QUESTION_LIMIT;
            case "BASIC":
                return BASIC_QUESTION_LIMIT;
            case "FREE":
            default:
                return FREE_QUESTION_LIMIT;
        }
    }
    
    /**
     * 남은 일일 문제 풀이 가능 개수 반환
     */
    public int getRemainingQuestions(User user) {
        resetDailyQuestionsIfNeeded(user);
        String tier = getCurrentTier(user);
        int limit = getTierLimit(tier);
        if (limit == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE; // 무제한
        }
        return limit - user.getQuestionsUsed();
    }
    
    /**
     * 문제 풀이 개수 증가 (일일 한도 체크 포함)
     */
    public void incrementQuestionCount(User user) {
        resetDailyQuestionsIfNeeded(user);
        user.setQuestionsUsed(user.getQuestionsUsed() + 1);
        userRepository.save(user);
    }
    
    /**
     * 문제 풀이 가능 여부 확인
     */
    public boolean canSolveQuestion(User user) {
        String tier = getCurrentTier(user);
        if ("ACADEMY".equals(tier)) {
            return true; // 무제한
        }
        return getRemainingQuestions(user) > 0;
    }
    
    /**
     * @deprecated 문제 생성은 이제 제한이 없습니다. canSolveQuestion을 사용하세요.
     */
    @Deprecated
    public boolean canGenerateQuestion(User user) {
        return true; // 문제 생성은 제한 없음
    }
    
    /**
     * 문제 해결 수에 따른 무료 체험 업그레이드 가능 여부 확인
     * @param user 사용자
     * @return 업그레이드 가능한 티어 (없으면 null)
     */
    public String checkAndUpgradeTier(User user) {
        int solved = user.getTotalQuestionsSolved();
        String currentTier = getCurrentTier(user);
        
        // 이미 유료 구독이 있으면 자동 업그레이드 불가
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        if (subscriptionOpt.isPresent()) {
            Subscription sub = subscriptionOpt.get();
            // 유료 구독이 활성화되어 있고 체험이 아니면 자동 업그레이드 불가
            if (sub.getIsActive() && !sub.getIsTrial()) {
                return null;
            }
        }
        
        // 무료 체험 업그레이드 가능 여부 확인 (문제 해결 수 기준)
        if ("FREE".equals(currentTier) && solved >= BASIC_THRESHOLD) {
            return "BASIC";
        } else if ("BASIC".equals(currentTier) && solved >= PREMIUM_THRESHOLD) {
            return "PREMIUM";
        } else if ("PREMIUM".equals(currentTier) && solved >= ACADEMY_THRESHOLD) {
            return "ACADEMY";
        }
        
        return null; // 업그레이드 불가
    }
    
    /**
     * 티어 업그레이드 실행 (무료 체험)
     * @param user 사용자
     * @param newTier 새로운 티어
     */
    @Transactional
    public void upgradeTier(User user, String newTier) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        Subscription subscription;
        
        if (subscriptionOpt.isPresent()) {
            subscription = subscriptionOpt.get();
            // 이미 유료 구독이 있으면 체험 업그레이드 불가
            if (subscription.getIsActive() && !subscription.getIsTrial()) {
                throw new RuntimeException("이미 유료 구독이 활성화되어 있습니다. 영구 업그레이드는 결제를 통해 가능합니다.");
            }
        } else {
            subscription = new Subscription();
            subscription.setUser(user);
        }
        
        subscription.setTier(newTier);
        subscription.setIsActive(true);
        subscription.setIsTrial(true); // 무료 체험으로 설정
        // 무료 체험 기간 설정 (3일)
        subscription.setExpiryDate(LocalDateTime.now().plusDays(TRIAL_PERIOD_DAYS));
        
        subscriptionRepository.save(subscription);
    }
    
    public void requestSubscription(String email) {
        // Check if there's already a pending request
        List<SubscriptionRequest> existingRequests = subscriptionRequestRepository
                .findByEmailAndStatus(email, "pending");
        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("이미 구독 신청이 진행 중입니다. 승인을 기다려주세요.");
        }
        
        SubscriptionRequest request = new SubscriptionRequest();
        request.setEmail(email);
        request.setStatus("pending");
        subscriptionRequestRepository.save(request);
    }
    
    @Transactional
    public void approveSubscription(Long requestId) {
        Optional<SubscriptionRequest> requestOpt = subscriptionRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            throw new RuntimeException("구독 요청을 찾을 수 없습니다.");
        }
        
        SubscriptionRequest request = requestOpt.get();
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        
        User user = userOpt.get();
        
        // Create or update subscription
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        Subscription subscription;
        if (subscriptionOpt.isPresent()) {
            subscription = subscriptionOpt.get();
        } else {
            subscription = new Subscription();
            subscription.setUser(user);
        }
        
        subscription.setIsActive(true);
        subscription.setIsTrial(false); // 결제를 통한 구독은 체험이 아님
        // 결제를 통한 구독은 PREMIUM 티어로 설정 (또는 요청에 따라)
        if (subscription.getTier() == null || "FREE".equals(subscription.getTier())) {
            subscription.setTier("PREMIUM"); // 결제 시 기본적으로 PREMIUM 티어
        }
        subscription.setExpiryDate(LocalDateTime.now().plusDays(30));
        subscriptionRepository.save(subscription);
        
        // Update request status
        request.setStatus("approved");
        subscriptionRequestRepository.save(request);
        
        // Send approval email
        emailService.sendSubscriptionApprovalEmail(user.getEmail());
    }
    
    @Transactional
    public void denySubscription(Long requestId) {
        Optional<SubscriptionRequest> requestOpt = subscriptionRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            throw new RuntimeException("구독 요청을 찾을 수 없습니다.");
        }
        
        SubscriptionRequest request = requestOpt.get();
        request.setStatus("denied");
        subscriptionRequestRepository.save(request);
        
        emailService.sendSubscriptionDenialEmail(request.getEmail());
    }
    
    public List<SubscriptionRequest> getPendingSubscriptions() {
        return subscriptionRequestRepository.findByStatus("pending");
    }
    
    private void unsubscribe(User user) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setIsActive(false);
            subscription.setExpiryDate(null);
            subscriptionRepository.save(subscription);
        }
    }
}

