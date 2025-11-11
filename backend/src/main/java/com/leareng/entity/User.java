package com.leareng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
    private Boolean isVerified = false;

    @Column(nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
    private Boolean isAdmin = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 일일 문제 풀이 개수 (매일 리셋됨)
     */
    @Column(nullable = false)
    private Integer questionsUsed = 0;

    /**
     * 마지막 문제 풀이 한도 리셋 시간
     */
    private LocalDateTime lastQuestionReset;

    @Column(columnDefinition = "NUMBER(10,0) DEFAULT 0")
    private Integer totalQuestionsSolved = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastQuestionReset == null) {
            lastQuestionReset = LocalDateTime.now();
        }
    }
}

