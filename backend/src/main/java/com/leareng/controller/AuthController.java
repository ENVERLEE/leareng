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
    
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmailByLink(@RequestParam String token, @RequestParam String email) {
        try {
            boolean verified = userService.verifyEmail(email, token);
            if (verified) {
                // HTML 페이지로 리다이렉트하거나 성공 메시지 반환
                String htmlResponse = """
                    <!DOCTYPE html>
                    <html lang="ko">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>이메일 인증 완료 - Leareng</title>
                        <link rel="stylesheet" href="css/styles.css">
                    </head>
                    <body>
                        <div class="auth-container">
                            <h1>이메일 인증 완료</h1>
                            <div style="text-align: center; padding: 2rem 0;">
                                <div style="font-size: 3rem; color: #10b981; margin-bottom: 1rem;">✓</div>
                                <h2 style="color: var(--text-primary); margin-bottom: 1rem;">인증이 완료되었습니다!</h2>
                                <p style="color: var(--text-secondary); margin-bottom: 2rem;">이제 로그인하여 서비스를 이용하실 수 있습니다.</p>
                                <a href="login.html" class="btn-primary" style="display: inline-block; text-decoration: none;">로그인하기</a>
                            </div>
                        </div>
                    </body>
                    </html>
                    """;
                return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8").body(htmlResponse);
            } else {
                String htmlResponse = """
                    <!DOCTYPE html>
                    <html lang="ko">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>이메일 인증 실패 - Leareng</title>
                        <link rel="stylesheet" href="css/styles.css">
                    </head>
                    <body>
                        <div class="auth-container">
                            <h1>이메일 인증 실패</h1>
                            <div style="text-align: center; padding: 2rem 0;">
                                <div style="font-size: 3rem; color: #ef4444; margin-bottom: 1rem;">✗</div>
                                <h2 style="color: var(--text-primary); margin-bottom: 1rem;">인증에 실패했습니다</h2>
                                <p style="color: var(--text-secondary); margin-bottom: 2rem;">유효하지 않거나 만료된 링크입니다. 새로운 인증 링크를 요청해주세요.</p>
                                <a href="login.html" class="btn-primary" style="display: inline-block; text-decoration: none;">로그인 페이지로 이동</a>
                            </div>
                        </div>
                    </body>
                    </html>
                    """;
                return ResponseEntity.badRequest().header("Content-Type", "text/html; charset=UTF-8").body(htmlResponse);
            }
        } catch (Exception e) {
            String htmlResponse = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>이메일 인증 오류 - Leareng</title>
                    <link rel="stylesheet" href="css/styles.css">
                </head>
                <body>
                    <div class="auth-container">
                        <h1>오류 발생</h1>
                        <div style="text-align: center; padding: 2rem 0;">
                            <div style="font-size: 3rem; color: #ef4444; margin-bottom: 1rem;">⚠</div>
                            <h2 style="color: var(--text-primary); margin-bottom: 1rem;">인증 중 오류가 발생했습니다</h2>
                            <p style="color: var(--text-secondary); margin-bottom: 2rem;">잠시 후 다시 시도해주세요.</p>
                            <a href="login.html" class="btn-primary" style="display: inline-block; text-decoration: none;">로그인 페이지로 이동</a>
                        </div>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.badRequest().header("Content-Type", "text/html; charset=UTF-8").body(htmlResponse);
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

