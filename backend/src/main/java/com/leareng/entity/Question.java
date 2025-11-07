package com.leareng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "passage_id", nullable = false)
    private Passage passage;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String choices; // JSON string of choices array

    @Column(nullable = false)
    private Integer correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    private Integer difficulty;

    private String type;

    private String typeId;

    @Column(columnDefinition = "TEXT")
    private String originalText;

    @Column(columnDefinition = "TEXT")
    private String koreanTranslation;

    private Integer questionNumber;
}

