package com.leareng.controller;

import com.leareng.dto.QuestionOutput;
import com.leareng.entity.Passage;
import com.leareng.entity.User;
import com.leareng.repository.PassageRepository;
import com.leareng.repository.UserRepository;
import com.leareng.service.*;
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
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {
    
    @Autowired
    private QuestionGeneratorService questionGeneratorService;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private PassageService passageService;
    
    @Autowired
    private PassageRepository passageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuestions(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            User user = userOpt.get();
            
            // Check subscription status
            if (!subscriptionService.canGenerateQuestion(user)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "무료 버전의 일일 문제 생성 한도를 초과했습니다. 구독을 신청해주세요.");
                return ResponseEntity.badRequest().body(error);
            }
            
            Long passageId = null;
            if (request.get("passageId") != null) {
                passageId = Long.parseLong(request.get("passageId").toString());
            }
            
            String passageText = (String) request.get("text");
            String title = (String) request.get("title");
            
            if (passageText == null || passageText.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "지문 내용은 필수입니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            Passage passage;
            if (passageId != null) {
                Optional<Passage> passageOpt = passageRepository.findById(passageId);
                if (passageOpt.isEmpty()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "지문을 찾을 수 없습니다.");
                    return ResponseEntity.badRequest().body(error);
                }
                passage = passageOpt.get();
                
                if (questionService.hasQuestions(passage)) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "이미 이 지문에 대한 문제가 생성되어 있습니다.");
                    return ResponseEntity.badRequest().body(error);
                }
            } else {
                if (title == null || title.isEmpty()) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "제목은 필수입니다.");
                    return ResponseEntity.badRequest().body(error);
                }
                passage = passageService.savePassage(title, passageText);
            }
            
            // Start async question generation
            questionGeneratorService.generateQuestionsAsync(passageId, passageText, title, passage, user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "문제 생성이 시작되었습니다. 잠시 후 문제 목록을 확인해주세요.");
            response.put("passageId", passage.getId());
            response.put("status", "processing");
            
            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "문제 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @GetMapping("/passage/{passageId}")
    public ResponseEntity<?> getQuestionsByPassage(@PathVariable Long passageId) {
        try {
            Optional<Passage> passageOpt = passageRepository.findById(passageId);
            if (passageOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "지문을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            List<QuestionOutput> questions = questionService.getQuestionsByPassage(passageOpt.get());
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "문제 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}

