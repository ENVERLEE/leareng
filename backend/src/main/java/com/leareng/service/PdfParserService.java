package com.leareng.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class PdfParserService {
    
    @Autowired
    private OpenAIApiService openAIApiService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public List<String> extractPassages(byte[] pdfBytes) throws IOException {
        // Extract raw text from PDF
        String rawText = extractTextFromPdf(pdfBytes);
        
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IOException("PDF에서 텍스트를 추출할 수 없습니다.");
        }
        
        // Use OpenAI to clean and extract English passages
        String systemMessage = """
            You are a helpful assistant that extracts and cleans English passages from PDFs.
            Your task is to:
            1. Identify and separate MULTIPLE English passages/articles from the input
            2. For each passage:
               - Remove any unnecessary whitespace, headers, footers, and page numbers
               - Ensure proper paragraph breaks
            3. Return the passages in this exact JSON format:
               {
                 "passages": [
                   {
                     "text": "First passage text...",
                     "word_count": number_of_words
                   },
                   {
                     "text": "Second passage text...",
                     "word_count": number_of_words
                   }
                 ]
               }
            4. Each passage should be complete and make sense on its own
            5. If there's no meaningful English text, return {"passages": []}
            """;
        
        String userMessage = "Please extract and clean the English passages from this PDF content:\n\n" + rawText;
        
        try {
            String response = openAIApiService.generateText(systemMessage, userMessage).block();
            
            // Extract JSON from response
            response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode passagesNode = jsonNode.get("passages");
            
            if (passagesNode == null || !passagesNode.isArray()) {
                throw new IOException("지문 추출 중 오류가 발생했습니다.");
            }
            
            List<String> passages = new ArrayList<>();
            for (JsonNode passageNode : passagesNode) {
                String text = passageNode.get("text").asText();
                int wordCount = passageNode.get("word_count").asInt();
                
                if (wordCount >= 10) { // Minimum 10 words
                    passages.add(text);
                }
            }
            
            if (passages.isEmpty()) {
                throw new IOException("추출된 지문이 너무 짧습니다.");
            }
            
            return passages;
        } catch (Exception e) {
            throw new IOException("PDF 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    private String extractTextFromPdf(byte[] pdfBytes) throws IOException {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    @Async
    public CompletableFuture<List<String>> extractPassagesAsync(byte[] pdfBytes) {
        try {
            List<String> passages = extractPassages(pdfBytes);
            return CompletableFuture.completedFuture(passages);
        } catch (Exception e) {
            System.err.println("Error extracting passages asynchronously: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }
}

