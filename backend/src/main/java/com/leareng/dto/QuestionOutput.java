package com.leareng.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QuestionOutput {
    private String question;
    private List<String> choices;
    
    @JsonProperty("correct_answer")
    private Integer correctAnswer;
    
    private String explanation;
    private Integer difficulty;
    
    @JsonProperty("key_points")
    private List<String> keyPoints;
    
    @JsonProperty("wrong_answer_analysis")
    private Map<String, String> wrongAnswerAnalysis;
    
    private String type;
    private String typeId;
    private Integer questionNumber;
    private String originalText;
    private String koreanTranslation;
}

