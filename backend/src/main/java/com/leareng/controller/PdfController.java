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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "*")
public class PdfController {
    
    @Autowired
    private PdfParserService pdfParserService;
    
    // PDF 처리 결과를 임시 저장하는 맵 (작업 ID -> 결과)
    private static final Map<String, PdfProcessingResult> processingResults = new ConcurrentHashMap<>();
    
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
            
            // 작업 ID 생성
            String jobId = UUID.randomUUID().toString();
            
            // 처리 중 상태로 초기화
            processingResults.put(jobId, new PdfProcessingResult("processing", null, null));
            
            // 비동기로 PDF 처리 시작
            byte[] pdfBytes = file.getBytes();
            pdfParserService.extractPassagesAsync(pdfBytes)
                .thenAccept(passages -> {
                    // 처리 완료 시 결과 저장
                    processingResults.put(jobId, new PdfProcessingResult("completed", passages, null));
                })
                .exceptionally(ex -> {
                    // 처리 실패 시 에러 저장
                    processingResults.put(jobId, new PdfProcessingResult("failed", null, ex.getMessage()));
                    return null;
                });
            
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("status", "processing");
            response.put("message", "PDF 파일 분석이 시작되었습니다. 잠시 후 결과를 확인해주세요.");
            
            return ResponseEntity.accepted().body(response);
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
    
    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getPdfProcessingStatus(@PathVariable String jobId) {
        PdfProcessingResult result = processingResults.get(jobId);
        
        if (result == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "작업을 찾을 수 없습니다.");
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", result.status);
        
        if ("completed".equals(result.status)) {
            response.put("passages", result.passages);
            response.put("count", result.passages != null ? result.passages.size() : 0);
            // 결과를 반환한 후 삭제 (선택사항)
            processingResults.remove(jobId);
        } else if ("failed".equals(result.status)) {
            response.put("error", result.error);
            processingResults.remove(jobId);
        }
        
        return ResponseEntity.ok(response);
    }
    
    // PDF 처리 결과를 저장하는 내부 클래스
    private static class PdfProcessingResult {
        String status;
        List<String> passages;
        String error;
        
        PdfProcessingResult(String status, List<String> passages, String error) {
            this.status = status;
            this.passages = passages;
            this.error = error;
        }
    }
}

