package com.leareng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
    private Boolean isActive = false;

    private LocalDateTime expiryDate;

    @Column(nullable = false, length = 20)
    private String tier = "FREE"; // FREE, BASIC, PREMIUM, ACADEMY

    @Column(nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0")
    private Boolean isTrial = false; // 무료 체험 여부
}

