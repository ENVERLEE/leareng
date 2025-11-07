package com.leareng.controller;

import com.leareng.entity.Passage;
import com.leareng.service.PassageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passages")
@CrossOrigin(origins = "*")
public class PassageController {
    
    @Autowired
    private PassageService passageService;
    
    @GetMapping
    public ResponseEntity<List<Passage>> getAllPassages() {
        return ResponseEntity.ok(passageService.getAllPassages());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Passage> getPassage(@PathVariable Long id) {
        return passageService.getPassageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<?> savePassage(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String text = request.get("text");
            
            if (title == null || title.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "제목은 필수입니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (text == null || text.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "지문 내용은 필수입니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            Passage passage = passageService.savePassage(title, text);
            return ResponseEntity.ok(passage);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "지문 저장 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}

