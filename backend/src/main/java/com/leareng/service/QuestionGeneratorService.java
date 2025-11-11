package com.leareng.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leareng.dto.FewShotExample;
import com.leareng.dto.QuestionOutput;
import com.leareng.entity.Passage;
import com.leareng.entity.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class QuestionGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(QuestionGeneratorService.class);
    
    @Autowired
    private OpenAIApiService openAIApiService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    private static final Map<String, Map<String, String>> QUESTION_TYPES = new HashMap<>();
    
    // Few-Shot 예시를 저장하는 맵 (문제 유형 -> 예시 리스트)
    private Map<String, List<FewShotExample>> fewShotExamples = new HashMap<>();
    
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
    
    /**
     * 애플리케이션 시작 시 JSON 파일에서 Few-Shot 예시를 로드
     */
    @PostConstruct
    public void loadFewShotExamples() {
        try {
            logger.info("Loading few-shot examples from few-shot-examples.json...");
            ClassPathResource resource = new ClassPathResource("few-shot-examples.json");
            if (!resource.exists()) {
                logger.warn("few-shot-examples.json file not found. Continuing without few-shot examples.");
                return;
            }
            
            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode rootNode = objectMapper.readTree(inputStream);
                
                // 각 문제 유형별로 예시 로드
                for (String questionType : QUESTION_TYPES.keySet()) {
                    JsonNode typeNode = rootNode.get(questionType);
                    if (typeNode != null && typeNode.has("examples")) {
                        JsonNode examplesNode = typeNode.get("examples");
                        if (examplesNode.isArray()) {
                            List<FewShotExample> examples = new ArrayList<>();
                            for (JsonNode exampleNode : examplesNode) {
                                FewShotExample example = objectMapper.treeToValue(exampleNode, FewShotExample.class);
                                examples.add(example);
                            }
                            // 최대 5개만 사용
                            fewShotExamples.put(questionType, examples.subList(0, Math.min(5, examples.size())));
                            logger.info("Loaded {} few-shot examples for type: {}", fewShotExamples.get(questionType).size(), questionType);
                        }
                    }
                }
                logger.info("Few-shot examples loading completed. Total types loaded: {}", fewShotExamples.size());
            }
        } catch (Exception e) {
            logger.error("Error loading few-shot examples: {}", e.getMessage(), e);
            // 예시 없이 계속 진행
        }
    }
    
    /**
     * Few-Shot 예시를 프롬프트 형식으로 변환
     */
    private String buildFewShotPrompt(String questionType) {
        List<FewShotExample> examples = fewShotExamples.get(questionType);
        if (examples == null || examples.isEmpty()) {
            return "";
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("\n[Few-Shot Examples]\n");
        prompt.append("아래는 동일한 유형의 기존 문제 예시입니다. 이 스타일과 품질을 참고하여 문제를 생성하세요.\n\n");
        
        for (int i = 0; i < examples.size(); i++) {
            FewShotExample example = examples.get(i);
            prompt.append(String.format("--- Example %d ---\n", i + 1));
            prompt.append("Passage: ").append(example.getPassage()).append("\n");
            prompt.append("Question: ").append(example.getQuestion()).append("\n");
            prompt.append("Choices:\n");
            
            if (example.getChoices() != null) {
                for (int j = 0; j < example.getChoices().size(); j++) {
                    prompt.append(String.format("  %d. %s\n", j + 1, example.getChoices().get(j)));
                }
            }
            
            prompt.append("Correct Answer: ").append(example.getCorrectAnswer()).append("\n");
            if (example.getExplanation() != null && !example.getExplanation().isEmpty()) {
                prompt.append("Explanation: ").append(example.getExplanation()).append("\n");
            }
            prompt.append("\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * 문제 유형에 따른 지문 표시 안내 반환
     */
    private String getPassageInstruction(String questionType) {
        return switch (questionType) {
            case "blank_inference" -> 
                "For blank inference questions, add markers like (A), (B), (C), (D), (E) or _____ where blanks should be placed in the passage.";
            case "sentence_insertion" -> 
                "For sentence insertion questions, add markers ( ① ), ( ② ), ( ③ ), ( ④ ), ( ⑤ ) at appropriate positions in the passage where sentences could be inserted.";
            case "order_arrangement" -> 
                "For paragraph order questions, add markers (A), (B), (C), (D), (E) at the beginning of each paragraph or section that needs to be arranged.";
            case "irrelevant_sentence" -> 
                "For irrelevant sentence questions, add markers ( ① ), ( ② ), ( ③ ), ( ④ ), ( ⑤ ) at the beginning of each sentence or paragraph to identify which one is irrelevant.";
            default -> 
                "Use appropriate markers (①, ②, ③, ④, ⑤ or (A), (B), (C), (D), (E)) in the passage as needed for the question type, following the style of the examples provided.";
        };
    }
    
    /**
     * 문제 유형에 따른 선택지 언어 안내 반환
     */
    private String getChoiceLanguageInstruction(String questionType) {
        return switch (questionType) {
            case "title_theme" -> 
                "Choices must be in ENGLISH (e.g., 'Theories of Tears', 'Glass and Power in Architecture') as titles are naturally in English.";
            case "blank_inference" -> 
                "Choices must be in ENGLISH (e.g., 'Though - Contrary to', 'Nevertheless - For instance') as blank fillers are naturally in English.";
            case "vocabulary" -> 
                "Choices should be in ENGLISH for vocabulary meaning questions, as they test English word meanings.";
            default -> 
                "Choices can be in Korean or English depending on what is more natural for the question type. Follow the style of the examples provided.";
        };
    }
    
    /**
     * 한 번에 5개의 문제를 생성 (토큰 절약)
     */
    public List<QuestionOutput> generateQuestions(String text, String qType) {
        logger.info("=== Starting question generation ===");
        logger.info("Question type: {}", qType);
        logger.info("Passage length: {} characters", text.length());
        
        if (!QUESTION_TYPES.containsKey(qType)) {
            logger.error("Unknown question type: {}", qType);
            throw new IllegalArgumentException("Unknown question type: " + qType);
        }
        
        Map<String, String> typeInfo = QUESTION_TYPES.get(qType);
        String systemMessage = typeInfo.get("system_message");
        String typeName = typeInfo.get("name");
        
        logger.info("Question type name: {}", typeName);
        
        // Few-Shot 예시 가져오기
        String fewShotPrompt = buildFewShotPrompt(qType);
        int fewShotExampleCount = fewShotExamples.getOrDefault(qType, Collections.emptyList()).size();
        logger.info("Few-shot examples available: {}", fewShotExampleCount);
        
        // 문제 유형에 따라 지문에 표시 추가 안내
        String passageInstruction = getPassageInstruction(qType);
        // 문제 유형에 따른 선택지 언어 안내
        String choiceLanguageInstruction = getChoiceLanguageInstruction(qType);
        
        logger.debug("Passage instruction: {}", passageInstruction);
        logger.debug("Choice language instruction: {}", choiceLanguageInstruction);
        
        String userMessage = String.format("""
            Create exactly 5 different %s questions based on the following passage for Korean SAT (SUNEUNG) English test.
            Each question should be unique and test different aspects of the passage.
            %s
            [Passage]
            %s
            
            IMPORTANT: When creating questions, you MUST modify the passage text to include appropriate markers:
            %s
            
            IMPORTANT: Choice language requirements:
            %s
            
            CRITICAL FORMATTING RULES:
            1. The ORIGINAL passage text MUST remain EXACTLY THE SAME for all 5 questions. DO NOT modify, shorten, or change any words in the original passage.
            2. ONLY add markers (like (A), (B), (C), (D), (E) or ①, ②, ③, ④, ⑤ or _____) to the passage WITHOUT changing any original text.
               Example for blank_inference: If original is "... We look forward to your positive reply.", add marker: "... We look forward to your positive reply (E)."
               Example for sentence_insertion: If original is "... important point. However, we must consider...", add marker: "... important point. ( ① ) However, we must consider..."
            3. Each of the 5 questions can have markers at DIFFERENT positions, but the ORIGINAL TEXT must be IDENTICAL.
            4. The "passage" field MUST contain the FULL original passage text with ONLY markers added (no text changes).
            5. The "question" field MUST be a separate question text that references the markers in the passage.
               Example for blank_inference: "빈칸 (E)에 들어갈 가장 적절한 말은?"
               Example for sentence_insertion: "다음 글의 빈칸 ( ① )에 들어갈 문장으로 가장 적절한 것은?"
            6. The passage and question are SEPARATE - the question should NOT be embedded in the passage.
            7. The question text must clearly reference the marker (e.g., "(E)", "( ① )", etc.) that appears in the passage.
            
            Generate response in EXACTLY this JSON format:
            {
                "questions": [
                    {
                        "passage": "표시가 포함된 전체 지문 (예: ... We look forward to your positive reply (E).)",
                        "question": "지문의 표시를 참조하는 문제 (예: 빈칸 (E)에 들어갈 가장 적절한 말은?)",
                        "choices": [
                            "선택지 1 (언어는 문제 유형에 따라 영어 또는 한글)",
                            "선택지 2",
                            "선택지 3",
                            "선택지 4",
                            "선택지 5"
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
                    // ... 4 more questions
                ]
            }
            
            Requirements:
            - Generate exactly 5 different questions
            - CRITICAL: The ORIGINAL passage text MUST be IDENTICAL for all 5 questions. DO NOT modify, shorten, expand, or change any words, sentences, or structure of the original passage.
            - ONLY add markers (①, ②, ③, ④, ⑤ or (A), (B), (C), (D), (E) or _____) to the passage WITHOUT changing the original text.
            - Each question can have markers at DIFFERENT positions, but the base passage text must be EXACTLY THE SAME.
            - Each question MUST include a "passage" field with the FULL original passage text (unchanged) with ONLY markers added at appropriate positions
            - Each question MUST include a "question" field that is a SEPARATE question text referencing the marker in the passage (e.g., "빈칸 (E)에 들어갈 가장 적절한 말은?")
            - The passage and question must be SEPARATE - do NOT embed the question in the passage
            - The question text must clearly reference the marker that appears in the passage (e.g., "(E)", "( ① )")
            - Question text must be in Korean
            - Explanation and other fields must be in Korean
            - correct_answer must be a number between 1 and 5
            - difficulty must be a number between 1 and 5
            - key_points must be an array of strings
            - wrong_answer_analysis must be an object with numbers as keys
            - Strictly follow the JSON format above
            - Follow the style and quality of the examples provided above
            - Each question should be unique and test different aspects (different marker positions, different questions)
            """, typeName, fewShotPrompt, text, passageInstruction, choiceLanguageInstruction);
        
        logger.info("Sending request to Cerebras API...");
        logger.debug("System message length: {} characters", systemMessage.length());
        logger.debug("User message length: {} characters", userMessage.length());
        
        try {
            String response = openAIApiService.generateText(systemMessage, userMessage).block();
            
            logger.info("Received response from Cerebras API. Response length: {} characters", response != null ? response.length() : 0);
            logger.debug("Raw API response (first 500 chars): {}", response != null && response.length() > 500 ? response.substring(0, 500) + "..." : response);
            
            // Extract JSON from response (might be wrapped in markdown code blocks)
            String originalResponse = response;
            response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            if (!originalResponse.equals(response)) {
                logger.debug("Removed markdown code blocks from response");
            }
            
            logger.debug("Parsing JSON response...");
            Map<String, Object> jsonResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionsList = (List<Map<String, Object>>) jsonResponse.get("questions");
            
            if (questionsList == null || questionsList.isEmpty()) {
                logger.error("No questions found in API response. Response keys: {}", jsonResponse.keySet());
                logger.error("Full response: {}", response);
                throw new RuntimeException("No questions generated in response");
            }
            
            logger.info("Successfully parsed {} questions from API response", questionsList.size());
            
            List<QuestionOutput> questions = new ArrayList<>();
            for (int i = 0; i < questionsList.size(); i++) {
                Map<String, Object> questionData = questionsList.get(i);
                logger.debug("Processing question {}/{}", i + 1, questionsList.size());
                QuestionOutput question = new QuestionOutput();
                question.setQuestion((String) questionData.get("question"));
                
                // 표시가 포함된 지문이 있으면 사용, 없으면 원본 지문 사용
                String passageWithMarkers = (String) questionData.get("passage");
                if (passageWithMarkers != null && !passageWithMarkers.isEmpty()) {
                    question.setOriginalText(passageWithMarkers);
                } else {
                    question.setOriginalText(text);
                }
                
                @SuppressWarnings("unchecked")
                List<String> choices = (List<String>) questionData.get("choices");
                question.setChoices(choices);
                
                if (questionData.get("correct_answer") instanceof Number) {
                    question.setCorrectAnswer(((Number) questionData.get("correct_answer")).intValue());
                }
                
                question.setExplanation((String) questionData.get("explanation"));
                
                if (questionData.get("difficulty") instanceof Number) {
                    question.setDifficulty(((Number) questionData.get("difficulty")).intValue());
                }
                
                @SuppressWarnings("unchecked")
                List<String> keyPoints = (List<String>) questionData.get("key_points");
                if (keyPoints == null && questionData.get("key_points") instanceof String) {
                    keyPoints = List.of((String) questionData.get("key_points"));
                }
                question.setKeyPoints(keyPoints);
                
                @SuppressWarnings("unchecked")
                Map<String, String> wrongAnswerAnalysis = (Map<String, String>) questionData.get("wrong_answer_analysis");
                if (wrongAnswerAnalysis == null) {
                    wrongAnswerAnalysis = new HashMap<>();
                }
                question.setWrongAnswerAnalysis(wrongAnswerAnalysis);
                
                question.setType(qType);
                question.setTypeId(typeName);
                
                questions.add(question);
                logger.debug("Question {} processed: type={}, hasPassage={}, choicesCount={}", 
                    i + 1, qType, question.getOriginalText() != null, 
                    question.getChoices() != null ? question.getChoices().size() : 0);
            }
            
            logger.info("=== Question generation completed successfully ===");
            logger.info("Generated {} questions for type: {}", questions.size(), qType);
            return questions;
        } catch (Exception e) {
            logger.error("=== Question generation failed ===");
            logger.error("Error generating questions for type {}: {}", qType, e.getMessage(), e);
            logger.error("Exception class: {}", e.getClass().getName());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to generate questions: " + e.getMessage(), e);
        }
    }
    
    public List<QuestionOutput> generateAllQuestions(String text) {
        logger.info("=== Starting generation of all questions ===");
        logger.info("Total question types: {}", QUESTION_TYPES.size());
        
        List<String> selectedTypes = new ArrayList<>(QUESTION_TYPES.keySet()).subList(0, Math.min(10, QUESTION_TYPES.size()));
        List<QuestionOutput> allQuestions = new ArrayList<>();
        
        logger.info("Generating questions for {} types", selectedTypes.size());
        
        int questionNumber = 1;
        // 각 유형당 한 번의 API 호출로 5개씩 문제 생성 (총 50개)
        for (int i = 0; i < selectedTypes.size(); i++) {
            String qType = selectedTypes.get(i);
            logger.info("Processing question type {}/{}: {}", i + 1, selectedTypes.size(), qType);
            
            try {
                List<QuestionOutput> questions = generateQuestions(text, qType);
                logger.info("Successfully generated {} questions for type: {}", questions.size(), qType);
                
                for (QuestionOutput question : questions) {
                    question.setQuestionNumber(questionNumber++);
                    allQuestions.add(question);
                }
            } catch (Exception e) {
                logger.error("Failed to generate questions for type {}: {}", qType, e.getMessage(), e);
                // Continue with other question types
            }
        }
        
        logger.info("=== All questions generation completed ===");
        logger.info("Total questions generated: {}/{} (expected: 50)", allQuestions.size(), selectedTypes.size() * 5);
        return allQuestions;
    }
    
    public String generateTranslation(String text) {
        logger.info("Generating Korean translation...");
        logger.debug("Original text length: {} characters", text != null ? text.length() : 0);
        
        String systemMessage = "You are a professional English to Korean translator.";
        String userMessage = String.format("""
            다음 영어 지문을 한국어로 번역해주세요. 자연스러운 한국어로 번역하되,
            원문의 의미와 뉘앙스를 정확하게 전달하는 것이 중요합니다:
            
            %s
            """, text);
        
        try {
            String translation = openAIApiService.generateText(systemMessage, userMessage).block();
            logger.info("Translation completed. Length: {} characters", translation != null ? translation.length() : 0);
            return translation;
        } catch (Exception e) {
            logger.error("Failed to generate translation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate translation: " + e.getMessage(), e);
        }
    }
    
    @Async
    public CompletableFuture<Void> generateQuestionsAsync(Long passageId, String passageText, String title, 
                                                          Passage passage, User user) {
        logger.info("=== Starting async question generation ===");
        logger.info("Passage ID: {}, Title: {}, User: {}", passageId, title, user.getEmail());
        logger.info("Passage text length: {} characters", passageText != null ? passageText.length() : 0);
        
        try {
            // Generate translation
            logger.info("Generating Korean translation...");
            String koreanTranslation = generateTranslation(passageText);
            logger.info("Translation completed. Length: {} characters", koreanTranslation != null ? koreanTranslation.length() : 0);
            
            // Generate questions
            logger.info("Generating all questions...");
            List<QuestionOutput> questions = generateAllQuestions(passageText);
            logger.info("Generated {} questions total", questions.size());
            
            // Save questions
            logger.info("Saving questions to database...");
            questionService.saveQuestions(passage, questions, passageText, koreanTranslation);
            logger.info("Questions saved successfully to database");
            
            logger.info("=== Async question generation completed successfully ===");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("=== Async question generation failed ===");
            logger.error("Error generating questions asynchronously for passage {}: {}", passageId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}

