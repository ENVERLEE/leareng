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

    @Column(nullable = false)
    private Integer questionsUsed = 0;

    private LocalDateTime lastQuestionReset;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastQuestionReset == null) {
            lastQuestionReset = LocalDateTime.now();
        }
    }
}

