package com.leareng.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailRequest {
    @NotBlank(message = "이메일은 필수입니다")
    private String email;
    
    @NotBlank(message = "인증 코드는 필수입니다")
    private String token;
}

