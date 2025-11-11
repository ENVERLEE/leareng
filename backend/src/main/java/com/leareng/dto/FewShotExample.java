package com.leareng.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FewShotExample {
    private String passage;
    private String question;
    private List<String> choices;
    
    @JsonProperty("correct_answer")
    private Integer correctAnswer;
    
    private String explanation;
}

