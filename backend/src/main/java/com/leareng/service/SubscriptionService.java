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
    
    private static final int FREE_QUESTION_LIMIT = 50;
    
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
    
    public void resetDailyQuestionsIfNeeded(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastReset = user.getLastQuestionReset();
        
        if (lastReset == null || now.toLocalDate().isAfter(lastReset.toLocalDate())) {
            user.setQuestionsUsed(0);
            user.setLastQuestionReset(now);
            userRepository.save(user);
        }
    }
    
    public int getRemainingQuestions(User user) {
        resetDailyQuestionsIfNeeded(user);
        return FREE_QUESTION_LIMIT - user.getQuestionsUsed();
    }
    
    public void incrementQuestionCount(User user) {
        resetDailyQuestionsIfNeeded(user);
        user.setQuestionsUsed(user.getQuestionsUsed() + 1);
        userRepository.save(user);
    }
    
    public boolean canGenerateQuestion(User user) {
        if (checkSubscriptionStatus(user)) {
            return true;
        }
        return getRemainingQuestions(user) > 0;
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

