package com.leareng.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIApiService.class);
    
    private WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${cerebras.api.key}")
    private String apiKey;
    
    @Value("${cerebras.api.url}")
    private String apiUrl;
    
    public OpenAIApiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void init() {
        logger.info("Initializing Cerebras API Service");
        logger.info("API URL: {}", apiUrl);
        logger.debug("API Key configured: {}", apiKey != null && !apiKey.isEmpty() ? "Yes" : "No");
        
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        
        logger.info("Cerebras API Service initialized successfully");
    }
    
    public Mono<String> generateText(String systemMessage, String userMessage) {
        logger.debug("=== Cerebras API Request ===");
        logger.debug("System message length: {} characters", systemMessage != null ? systemMessage.length() : 0);
        logger.debug("User message length: {} characters", userMessage != null ? userMessage.length() : 0);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-oss-120b");
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 65536);
        requestBody.put("temperature", 1);
        requestBody.put("top_p", 1);
        requestBody.put("reasoning_effort", "medium");
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemMessage);
        messages.add(systemMsg);
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        
        requestBody.put("messages", messages);
        
        logger.info("Sending request to Cerebras API: model=gpt-oss-120b, max_tokens=65536");
        logger.debug("Request URL: {}/chat/completions", apiUrl);
        
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                logger.error("=== Cerebras API Error ===");
                                logger.error("HTTP Status: {}", response.statusCode());
                                logger.error("Error response body: {}", errorBody);
                                
                                try {
                                    JsonNode errorNode = objectMapper.readTree(errorBody);
                                    String errorMessage = errorNode.path("error").path("message").asText();
                                    if (errorMessage.isEmpty()) {
                                        errorMessage = errorNode.path("error").asText();
                                    }
                                    if (errorMessage.isEmpty()) {
                                        errorMessage = errorBody;
                                    }
                                    logger.error("Parsed error message: {}", errorMessage);
                                    return Mono.error(new RuntimeException("Cerebras API Error (" + response.statusCode() + "): " + errorMessage));
                                } catch (Exception e) {
                                    logger.error("Failed to parse error response: {}", e.getMessage());
                                    return Mono.error(new RuntimeException("Cerebras API Error (" + response.statusCode() + "): " + errorBody));
                                }
                            });
                })
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    logger.debug("Received response from Cerebras API");
                    logger.debug("Response structure: {}", response.toPrettyString().substring(0, Math.min(500, response.toPrettyString().length())));
                    
                    JsonNode choices = response.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode message = choices.get(0).path("message");
                        JsonNode content = message.path("content");
                        if (content.isMissingNode() || content.asText().isEmpty()) {
                            logger.error("Cerebras API returned empty content");
                            throw new RuntimeException("Cerebras API returned empty content");
                        }
                        
                        String responseText = content.asText();
                        logger.info("Successfully extracted response content. Length: {} characters", responseText.length());
                        logger.debug("Response content (first 500 chars): {}", 
                            responseText.length() > 500 ? responseText.substring(0, 500) + "..." : responseText);
                        
                        return responseText;
                    }
                    logger.error("Invalid response from Cerebras API: no choices found");
                    logger.error("Response: {}", response.toPrettyString());
                    throw new RuntimeException("Invalid response from Cerebras API: no choices found");
                })
                .doOnError(error -> {
                    logger.error("=== Cerebras API Request Failed ===");
                    logger.error("Error type: {}", error.getClass().getName());
                    logger.error("Error message: {}", error.getMessage());
                    if (error.getCause() != null) {
                        logger.error("Caused by: {}", error.getCause().getMessage());
                    }
                    logger.debug("Request body: {}", requestBody);
                });
    }
}

