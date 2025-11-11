package com.leareng.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leareng.dto.QuestionOutput;
import com.leareng.entity.Passage;
import com.leareng.entity.Question;
import com.leareng.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Transactional
    public void saveQuestions(Passage passage, List<QuestionOutput> questionOutputs, String originalText, String koreanTranslation) {
        for (QuestionOutput qo : questionOutputs) {
            Question question = new Question();
            question.setPassage(passage);
            question.setQuestion(qo.getQuestion());
            
            try {
                question.setChoices(objectMapper.writeValueAsString(qo.getChoices()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize choices", e);
            }
            
            question.setCorrectAnswer(qo.getCorrectAnswer());
            question.setExplanation(qo.getExplanation());
            question.setDifficulty(qo.getDifficulty());
            question.setType(qo.getType());
            question.setTypeId(qo.getTypeId());
            question.setQuestionNumber(qo.getQuestionNumber());
            // 표시가 포함된 지문이 있으면 사용, 없으면 원본 지문 사용
            question.setOriginalText(qo.getOriginalText() != null && !qo.getOriginalText().isEmpty() 
                ? qo.getOriginalText() : originalText);
            question.setKoreanTranslation(koreanTranslation);
            
            questionRepository.save(question);
        }
    }
    
    public List<QuestionOutput> getQuestionsByPassage(Passage passage) {
        List<Question> questions = questionRepository.findByPassage(passage);
        
        return questions.stream().map(q -> {
            QuestionOutput qo = new QuestionOutput();
            qo.setId(q.getId());
            qo.setQuestion(q.getQuestion());
            
            try {
                @SuppressWarnings("unchecked")
                List<String> choices = objectMapper.readValue(q.getChoices(), List.class);
                qo.setChoices(choices);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize choices", e);
            }
            
            qo.setCorrectAnswer(q.getCorrectAnswer());
            qo.setExplanation(q.getExplanation());
            qo.setDifficulty(q.getDifficulty());
            qo.setType(q.getType());
            qo.setTypeId(q.getTypeId());
            qo.setQuestionNumber(q.getQuestionNumber());
            qo.setOriginalText(q.getOriginalText());
            qo.setKoreanTranslation(q.getKoreanTranslation());
            
            return qo;
        }).collect(Collectors.toList());
    }
    
    public boolean hasQuestions(Passage passage) {
        return questionRepository.existsByPassage(passage);
    }
}

