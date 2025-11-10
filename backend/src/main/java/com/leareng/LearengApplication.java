package com.leareng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LearengApplication {
    public static void main(String[] args) {
        SpringApplication.run(LearengApplication.class, args);
    }
}

