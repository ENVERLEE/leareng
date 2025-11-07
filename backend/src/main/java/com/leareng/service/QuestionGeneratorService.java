package com.leareng.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leareng.dto.QuestionOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class QuestionGeneratorService {
    
    @Autowired
    private OpenAIApiService openAIApiService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final Map<String, Map<String, String>> QUESTION_TYPES = new HashMap<>();
    
    static {
        QUESTION_TYPES.put("title_theme", Map.of(
            "name", "Title/Theme Inference",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Title/Theme Inference questions, strictly adhere to these principles:
                
                [Core Principles]
                1. Theme Selection - Must encompass the entire passage's unity and coherence
                2. Question Construction - Evaluate understanding of overall text flow
                3. Answer Choice Design - Create plausible but clearly incorrect alternatives
                4. Difficulty Calibration - Consider explicit vs implicit themes
                """
        ));
        
        QUESTION_TYPES.put("blank_inference", Map.of(
            "name", "Blank Inference",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Blank Inference questions, strictly adhere to these principles:
                
                [Core Principles]
                1. Logical Flow - Ensure clear logical progression
                2. Question Design - Strategic blank placement at key points
                3. Answer Choice Creation - Logically plausible alternatives
                4. Assessment Criteria - Logical reasoning ability
                """
        ));
        
        QUESTION_TYPES.put("sentence_insertion", Map.of(
            "name", "Sentence Insertion",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Sentence Insertion questions, focus on these elements:
                
                [Core Principles]
                1. Coherence Assessment - Logical flow maintenance
                2. Connection Analysis - Forward and backward linking
                3. Position Justification - Clear reasoning for placement
                4. Error Analysis - Identification of flow breaks
                """
        ));
        
        QUESTION_TYPES.put("order_arrangement", Map.of(
            "name", "Paragraph Order",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Paragraph Order questions, focus on these elements:
                
                [Core Principles]
                1. Logical Sequence - Clear progression of ideas
                2. Transitional Elements - Connective devices
                3. Structural Analysis - Opening paragraph identification
                4. Coherence Markers - Signal words and phrases
                """
        ));
        
        QUESTION_TYPES.put("vocabulary", Map.of(
            "name", "Vocabulary in Context",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Vocabulary in Context questions, focus on these elements:
                
                [Core Principles]
                1. Contextual Meaning - Word usage in specific context
                2. Distractor Design - Common misinterpretations
                3. Context Analysis - Surrounding phrase analysis
                4. Usage Evaluation - Collocational appropriateness
                """
        ));
        
        QUESTION_TYPES.put("main_idea", Map.of(
            "name", "Main Idea/Argument",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Main Idea/Argument questions, focus on these elements:
                
                [Core Principles]
                1. Central Concept Identification - Primary argument recognition
                2. Supporting Detail Analysis - Evidence evaluation
                3. Author's Purpose - Intention analysis
                4. Argument Evaluation - Logical consistency
                """
        ));
        
        QUESTION_TYPES.put("implied_meaning", Map.of(
            "name", "Implied Meaning",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Implied Meaning questions, focus on these elements:
                
                [Core Principles]
                1. Inference Skills - Reading between lines
                2. Evidence Collection - Textual support identification
                3. Logical Deduction - Reasonable conclusion drawing
                4. Alternative Interpretation - Multiple perspective consideration
                """
        ));
        
        QUESTION_TYPES.put("irrelevant_sentence", Map.of(
            "name", "Irrelevant Sentence",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Irrelevant Sentence questions, focus on these elements:
                
                [Core Principles]
                1. Unity Analysis - Topic consistency
                2. Context Evaluation - Local coherence
                3. Disruption Identification - Topic drift detection
                4. Relevance Assessment - Main idea alignment
                """
        ));
        
        QUESTION_TYPES.put("paragraph_summary", Map.of(
            "name", "Paragraph Summary",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Paragraph Summary questions, focus on these elements:
                
                [Core Principles]
                1. Main Point Extraction - Central idea identification
                2. Conciseness - Essential information selection
                3. Accuracy - Factual correctness
                4. Comprehensiveness - Key point inclusion
                """
        ));
        
        QUESTION_TYPES.put("reading_comprehension", Map.of(
            "name", "Reading Comprehension",
            "system_message", """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Overall Reading Comprehension questions, focus on these elements:
                
                [Core Principles]
                1. Comprehensive Understanding - Main idea grasp
                2. Critical Analysis - Evidence evaluation
                3. Detail Integration - Information synthesis
                4. Application Skills - Inference making
                """
        ));
    }
    
    public QuestionOutput generateQuestion(String text, String qType) {
        if (!QUESTION_TYPES.containsKey(qType)) {
            throw new IllegalArgumentException("Unknown question type: " + qType);
        }
        
        Map<String, String> typeInfo = QUESTION_TYPES.get(qType);
        String systemMessage = typeInfo.get("system_message");
        String typeName = typeInfo.get("name");
        
        String userMessage = String.format("""
            Create a %s question based on the following passage for Korean SAT (SUNEUNG) English test.
            
            [Passage]
            %s
            
            Generate response in EXACTLY this JSON format:
            {
                "question": "문제 내용 (한글)",
                "choices": [
                    "1번 선택지 (한글)",
                    "2번 선택지 (한글)",
                    "3번 선택지 (한글)",
                    "4번 선택지 (한글)",
                    "5번 선택지 (한글)"
                ],
                "correct_answer": 1,
                "explanation": "자세한 설명 (한글)",
                "difficulty": 3,
                "key_points": [
                    "핵심 포인트 1",
                    "핵심 포인트 2"
                ],
                "wrong_answer_analysis": {
                    "2": "2번 오답 분석",
                    "3": "3번 오답 분석",
                    "4": "4번 오답 분석",
                    "5": "5번 오답 분석"
                }
            }
            
            Requirements:
            - All text must be in Korean
            - correct_answer must be a number between 1 and 5
            - difficulty must be a number between 1 and 5
            - key_points must be an array of strings
            - wrong_answer_analysis must be an object with numbers as keys
            - Strictly follow the JSON format above
            """, typeName, text);
        
        try {
            String response = openAIApiService.generateText(systemMessage, userMessage).block();
            
            // Extract JSON from response (might be wrapped in markdown code blocks)
            response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            Map<String, Object> jsonResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            
            QuestionOutput question = new QuestionOutput();
            question.setQuestion((String) jsonResponse.get("question"));
            
            @SuppressWarnings("unchecked")
            List<String> choices = (List<String>) jsonResponse.get("choices");
            question.setChoices(choices);
            
            if (jsonResponse.get("correct_answer") instanceof Number) {
                question.setCorrectAnswer(((Number) jsonResponse.get("correct_answer")).intValue());
            }
            
            question.setExplanation((String) jsonResponse.get("explanation"));
            
            if (jsonResponse.get("difficulty") instanceof Number) {
                question.setDifficulty(((Number) jsonResponse.get("difficulty")).intValue());
            }
            
            @SuppressWarnings("unchecked")
            List<String> keyPoints = (List<String>) jsonResponse.get("key_points");
            if (keyPoints == null && jsonResponse.get("key_points") instanceof String) {
                keyPoints = List.of((String) jsonResponse.get("key_points"));
            }
            question.setKeyPoints(keyPoints);
            
            @SuppressWarnings("unchecked")
            Map<String, String> wrongAnswerAnalysis = (Map<String, String>) jsonResponse.get("wrong_answer_analysis");
            if (wrongAnswerAnalysis == null) {
                wrongAnswerAnalysis = new HashMap<>();
            }
            question.setWrongAnswerAnalysis(wrongAnswerAnalysis);
            
            question.setType(qType);
            question.setTypeId(typeName);
            
            return question;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate question: " + e.getMessage(), e);
        }
    }
    
    public List<QuestionOutput> generateAllQuestions(String text) {
        List<String> selectedTypes = new ArrayList<>(QUESTION_TYPES.keySet()).subList(0, Math.min(10, QUESTION_TYPES.size()));
        List<QuestionOutput> allQuestions = new ArrayList<>();
        
        for (int i = 0; i < selectedTypes.size(); i++) {
            String qType = selectedTypes.get(i);
            try {
                QuestionOutput question = generateQuestion(text, qType);
                question.setQuestionNumber(i + 1);
                allQuestions.add(question);
            } catch (Exception e) {
                // Log error but continue with other question types
                System.err.println("Failed to generate question type " + qType + ": " + e.getMessage());
            }
        }
        
        return allQuestions;
    }
    
    public String generateTranslation(String text) {
        String systemMessage = "You are a professional English to Korean translator.";
        String userMessage = String.format("""
            다음 영어 지문을 한국어로 번역해주세요. 자연스러운 한국어로 번역하되,
            원문의 의미와 뉘앙스를 정확하게 전달하는 것이 중요합니다:
            
            %s
            """, text);
        
        try {
            return openAIApiService.generateText(systemMessage, userMessage).block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate translation: " + e.getMessage(), e);
        }
    }
}

