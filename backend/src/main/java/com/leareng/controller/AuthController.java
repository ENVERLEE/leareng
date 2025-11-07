package com.leareng.controller;

import com.leareng.dto.*;
import com.leareng.entity.User;
import com.leareng.security.JwtUtil;
import com.leareng.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(request.getEmail(), request.getPassword());
            Map<String, String> response = new HashMap<>();
            response.put("message", "인증 이메일이 발송되었습니다. 이메일을 확인해주세요.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            String token = userService.login(request.getEmail(), request.getPassword());
            User user = userService.findByEmail(request.getEmail()).orElseThrow();
            
            AuthResponse response = new AuthResponse(token, user.getEmail(), user.getIsAdmin());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            boolean verified = userService.verifyEmail(request.getEmail(), request.getToken());
            if (verified) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "이메일이 성공적으로 인증되었습니다!");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "유효하지 않거나 만료된 인증 코드입니다.");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "이메일 인증 중 오류가 발생했습니다.");
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/reset-password-request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "이메일은 필수입니다.");
                return ResponseEntity.badRequest().body(error);
            }
            userService.requestPasswordReset(email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "비밀번호 재설정 이메일이 발송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            boolean reset = userService.resetPassword(request.getToken(), request.getNewPassword());
            if (reset) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "비밀번호가 성공적으로 재설정되었습니다!");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "유효하지 않거나 만료된 토큰입니다.");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "비밀번호 재설정 중 오류가 발생했습니다.");
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "이메일은 필수입니다.");
                return ResponseEntity.badRequest().body(error);
            }
            userService.resendVerificationEmail(email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "인증 이메일이 재발송되었습니다.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

