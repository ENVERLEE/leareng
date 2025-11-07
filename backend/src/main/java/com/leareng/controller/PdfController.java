package com.leareng.controller;

import com.leareng.service.PdfParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "*")
public class PdfController {
    
    @Autowired
    private PdfParserService pdfParserService;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "파일이 비어있습니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (!file.getContentType().equals("application/pdf")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "PDF 파일만 업로드 가능합니다.");
                return ResponseEntity.badRequest().body(error);
            }
            
            List<String> passages = pdfParserService.extractPassages(file.getBytes());
            
            Map<String, Object> response = new HashMap<>();
            response.put("passages", passages);
            response.put("count", passages.size());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "PDF 파일 분석 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}

