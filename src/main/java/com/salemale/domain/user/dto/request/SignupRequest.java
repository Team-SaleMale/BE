package com.salemale.domain.user.dto.request; // 회원가입 요청 바디 DTO

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern; // 정규식 검증(비밀번호 정책)
import lombok.Getter;

@Getter
public class SignupRequest { // /api/auth/signup 에서 사용하는 입력 모델

    @Email
    @NotBlank
    private String email; // 가입 이메일(로컬 로그인에 사용)

    @NotBlank
    @Size(min = 2, max = 15)
    private String nickname; // 표시 이름

    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,64}$",
            message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String password; // 원문 비밀번호(서버에서 해시 처리). 위 @Pattern으로 복합 규칙을 강제
}