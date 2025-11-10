package com.leareng.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
    
    private WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.url}")
    private String apiUrl;
    
    public OpenAIApiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
    
    public Mono<String> generateText(String systemMessage, String userMessage) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-5-nano");
        
        // gpt-5-nano 모델에 필요한 파라미터
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "text");
        requestBody.put("response_format", responseFormat);
        requestBody.put("verbosity", "medium");
        requestBody.put("reasoning_effort", "medium");
        requestBody.put("store", false);
        
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
        
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                try {
                                    JsonNode errorNode = objectMapper.readTree(errorBody);
                                    String errorMessage = errorNode.path("error").path("message").asText();
                                    if (errorMessage.isEmpty()) {
                                        errorMessage = errorNode.path("error").asText();
                                    }
                                    if (errorMessage.isEmpty()) {
                                        errorMessage = errorBody;
                                    }
                                    return Mono.error(new RuntimeException("OpenAI API Error (" + response.statusCode() + "): " + errorMessage));
                                } catch (Exception e) {
                                    return Mono.error(new RuntimeException("OpenAI API Error (" + response.statusCode() + "): " + errorBody));
                                }
                            });
                })
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode choices = response.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode message = choices.get(0).path("message");
                        JsonNode content = message.path("content");
                        if (content.isMissingNode() || content.asText().isEmpty()) {
                            throw new RuntimeException("OpenAI API returned empty content");
                        }
                        return content.asText();
                    }
                    throw new RuntimeException("Invalid response from OpenAI API: no choices found");
                })
                .doOnError(error -> {
                    System.err.println("OpenAI API request failed:");
                    System.err.println("Request body: " + requestBody);
                    System.err.println("Error: " + error.getMessage());
                });
    }
}

