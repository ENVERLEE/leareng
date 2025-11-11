package com.leareng.service;

import com.leareng.entity.User;
import com.leareng.entity.VerificationToken;
import com.leareng.repository.UserRepository;
import com.leareng.repository.VerificationTokenRepository;
import com.leareng.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VerificationTokenRepository tokenRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private EmailService emailService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Transactional
    public User registerUser(String email, String password) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // 이미 인증된 사용자인 경우
            if (user.getIsVerified()) {
                throw new RuntimeException("이미 등록된 이메일입니다.");
            }
            // 미인증 사용자인 경우 기존 데이터 삭제
            tokenRepository.deleteByEmail(email);
            userRepository.delete(user);
            // 삭제를 즉시 데이터베이스에 반영
            entityManager.flush();
            entityManager.clear();
        }
        
        logger.info("Creating new user for email: {}", email);
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setIsVerified(false);
        user.setIsAdmin(false);
        logger.info("Saving user to database...");
        user = userRepository.save(user);
        logger.info("User saved successfully with ID: {}", user.getId());
        
        // Generate verification token
        logger.info("Generating verification token...");
        String token = jwtUtil.generateToken(email);
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setEmail(email);
        verificationToken.setToken(token);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        logger.info("Saving verification token...");
        tokenRepository.save(verificationToken);
        logger.info("Verification token saved successfully");
        
        // Send verification email
        logger.info("About to send verification email to: {}", email);
        try {
            logger.info("Attempting to send verification email to: {}", email);
            emailService.sendVerificationEmail(email, token);
            logger.info("Verification email sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", email, e.getMessage(), e);
            // Don't fail the registration if email sending fails
        }
        logger.info("Registration process completed for: {}", email);
        
        return user;
    }
    
    public String login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        
        User user = userOpt.get();
        if (!user.getIsVerified() && !user.getIsAdmin()) {
            throw new RuntimeException("이메일 인증이 필요합니다. 이메일을 확인해주세요.");
        }
        
        return jwtUtil.generateToken(email);
    }
    
    @Transactional
    public boolean verifyEmail(String email, String token) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        VerificationToken verificationToken = tokenOpt.get();
        if (!verificationToken.getEmail().equals(email) || 
            verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        user.setIsVerified(true);
        userRepository.save(user);
        
        tokenRepository.delete(verificationToken);
        return true;
    }
    
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("등록되지 않은 이메일입니다.");
        }
        
        // Delete old tokens
        tokenRepository.deleteByEmail(email);
        
        // Generate new token
        String token = jwtUtil.generateToken(email);
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setEmail(email);
        verificationToken.setToken(token);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        tokenRepository.save(verificationToken);
        
        emailService.sendPasswordResetEmail(email, token);
    }
    
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        VerificationToken verificationToken = tokenOpt.get();
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findByEmail(verificationToken.getEmail());
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        tokenRepository.delete(verificationToken);
        return true;
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public void resendVerificationEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("등록되지 않은 이메일입니다.");
        }
        
        User user = userOpt.get();
        if (user.getIsVerified()) {
            throw new RuntimeException("이미 인증된 계정입니다.");
        }
        
        // Delete old tokens
        tokenRepository.deleteByEmail(email);
        
        // Generate new token
        String token = jwtUtil.generateToken(email);
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setEmail(email);
        verificationToken.setToken(token);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        tokenRepository.save(verificationToken);
        
        // Resend verification email
        emailService.sendVerificationEmail(email, token);
    }
}

