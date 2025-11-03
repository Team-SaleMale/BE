package com.salemale.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class SignupRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 2, max = 15)
    private String nickname;

    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,64}$",
            message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String password;

    // 로컬/소셜 공통 회원가입 시 필요한 지역 식별자(프론트에서 선택)
    @NotNull
    private Long regionId;
}

