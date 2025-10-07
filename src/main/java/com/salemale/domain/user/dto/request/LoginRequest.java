package com.salemale.domain.user.dto.request; // 로그인 요청 바디 DTO

import jakarta.validation.constraints.Email; // 이메일 형식 검증 애노테이션
import jakarta.validation.constraints.NotBlank; // 빈 문자열 금지 검증
import lombok.Getter; // 게터 생성

@Getter
public class LoginRequest { // /api/auth/login 에서 사용하는 입력 모델

    @Email // RFC 이메일 형식 검사
    @NotBlank // null/빈문자열 방지
    private String email;

    @NotBlank // null/빈문자열 방지
    private String password;
}


