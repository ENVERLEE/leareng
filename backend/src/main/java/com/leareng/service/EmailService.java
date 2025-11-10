package com.leareng.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:80}")
    private String frontendUrl;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    public void sendEmail(String to, String subject, String body) {
        try {
            logger.info("Attempting to send email to: {} with subject: {}", to, subject);
            logger.info("Using from email: {}", fromEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true indicates HTML
            
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("MessagingException while sending email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
    
    public void sendVerificationEmail(String to, String token) {
        String subject = "Leareng 이메일 인증";
        String verificationLink = frontendUrl + "/verify-email.html?token=" + token + "&email=" + to;
        String body = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #333; margin-bottom: 20px;">Leareng 이메일 인증</h2>
                <p style="color: #666; line-height: 1.6; margin-bottom: 20px;">안녕하세요,</p>
                <p style="color: #666; line-height: 1.6; margin-bottom: 20px;">이메일 인증을 완료하려면 아래 링크를 클릭해주세요:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="display: inline-block; padding: 12px 30px; background-color: #6366F1; color: #FFFFFF; text-decoration: none; border-radius: 8px; font-weight: 600;">이메일 인증하기</a>
                </div>
                <p style="color: #999; font-size: 12px; margin-top: 20px;">위 버튼이 작동하지 않는 경우, 아래 링크를 복사하여 브라우저에 붙여넣으세요:</p>
                <p style="color: #666; font-size: 12px; word-break: break-all; background-color: #f5f5f5; padding: 10px; border-radius: 4px;">%s</p>
                <p style="color: #999; font-size: 12px; margin-top: 20px;">이 링크는 24시간 동안 유효합니다.</p>
            </div>
            """, verificationLink, verificationLink);
        sendEmail(to, subject, body);
    }
    
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Leareng 비밀번호 재설정";
        String resetLink = frontendUrl + "/reset-password.html?token=" + token;
        String body = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #333; margin-bottom: 20px;">Leareng 비밀번호 재설정</h2>
                <p style="color: #666; line-height: 1.6; margin-bottom: 20px;">안녕하세요,</p>
                <p style="color: #666; line-height: 1.6; margin-bottom: 20px;">비밀번호를 재설정하려면 아래 링크를 클릭해주세요:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="display: inline-block; padding: 12px 30px; background-color: #6366F1; color: #FFFFFF; text-decoration: none; border-radius: 8px; font-weight: 600;">비밀번호 재설정하기</a>
                </div>
                <p style="color: #999; font-size: 12px; margin-top: 20px;">위 버튼이 작동하지 않는 경우, 아래 링크를 복사하여 브라우저에 붙여넣으세요:</p>
                <p style="color: #666; font-size: 12px; word-break: break-all; background-color: #f5f5f5; padding: 10px; border-radius: 4px;">%s</p>
                <p style="color: #999; font-size: 12px; margin-top: 20px;">이 링크는 24시간 동안 유효합니다.</p>
            </div>
            """, resetLink, resetLink);
        sendEmail(to, subject, body);
    }
    
    public void sendSubscriptionApprovalEmail(String to) {
        String subject = "Leareng 구독 승인";
        String body = """
            <h2>Leareng 구독 승인</h2>
            <p>회원님의 구독 요청이 승인되었습니다.</p>
            <p>이제 프리미엄 기능을 이용하실 수 있습니다.</p>
            """;
        sendEmail(to, subject, body);
    }
    
    public void sendSubscriptionDenialEmail(String to) {
        String subject = "Leareng 구독 거절 알림";
        String body = """
            <h2>Leareng 구독 거절 알림</h2>
            <p>죄송합니다. 회원님의 구독 요청이 거절되었습니다.</p>
            <p>자세한 사항은 고객센터로 문의해주세요.</p>
            """;
        sendEmail(to, subject, body);
    }
}

