package com.salemale.domain.user.controller; // 인증 HTTP 엔드포인트 정의(로그인/회원가입/로그아웃 등)

import com.salemale.common.response.ApiResponse; // 통일된 API 응답 포맷 래퍼
import com.salemale.domain.user.dto.request.LoginRequest; // 로그인 요청 DTO(email/password)
import com.salemale.domain.user.dto.request.SignupRequest; // 회원가입 요청 DTO(email/nickname/password)
import com.salemale.domain.user.service.AuthService; // 인증 비즈니스 로직 서비스(로그인/회원가입 등)
import jakarta.validation.Valid; // 요청 바디 검증
import org.springframework.http.ResponseEntity; // HTTP 응답 래퍼
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑
import org.springframework.web.bind.annotation.PatchMapping; // PATCH 매핑(로그아웃 등 상태변경용)
import org.springframework.web.bind.annotation.RequestBody; // 요청 바디 바인딩
import org.springframework.web.bind.annotation.RequestMapping; // 베이스 경로 매핑
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 선언

import java.util.Map; // 간단한 키/값 응답을 위해 사용

@RestController
@RequestMapping("/auth") // 모든 인증 관련 경로는 /auth 하위로 통일(/auth/login, /auth/register, /auth/logout)
public class AuthController { // 인증 관련 엔드포인트 집합(초심자도 이해할 수 있도록 상세 주석 포함)

    private final AuthService authService;

    public AuthController(AuthService authService) { // 생성자를 통해 서비스 의존성 주입
        this.authService = authService; // 스프링이 AuthService 빈을 자동으로 넣어줍니다.
    }

    @PostMapping("/login") // 로그인: 사용자가 이메일/비밀번호를 보내면 서버가 확인 후 토큰을 발급해 줍니다.
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
        // 1) @Valid: request에 적힌 @Email, @NotBlank 등의 검사를 먼저 수행합니다.
        // 2) 서비스에 로그인 요청: 비밀번호 해시 검증에 성공하면 JWT 토큰이 문자열로 돌아옵니다.
        String token = authService.loginLocal(request.getEmail(), request.getPassword());
        // 3) ApiResponse.onSuccess로 성공 응답을 표준 형태로 감싸서 반환합니다.
        //    응답 본문 예시: { "isSuccess": true, "code": "200", "message": "OK", "result": { "accessToken": "..." } }
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of("accessToken", token)));
    }

    @PostMapping("/register") // 회원가입: 새 사용자를 만들고 로컬 자격(이메일/비번)을 저장합니다.
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody SignupRequest request) {
        // 1) 이메일/닉네임/비밀번호 입력 검증(@Valid)
        // 2) 서비스에 위임: 중복 이메일 검사 → User 생성 → UserAuth(LOCAL) 생성(비밀번호 해시)
        authService.registerLocal(request);
        // 3) 생성 성공 시 바디 없이 성공 응답을 반환합니다.
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    @PatchMapping("/logout") // 로그아웃: JWT는 서버가 상태를 저장하지 않으므로 보통 클라이언트에서 토큰을 버립니다.
    public ResponseEntity<ApiResponse<Void>> logout() {
        // 서버 세션을 쓰지 않는 JWT 구조에서는 서버가 무언가 지울 상태가 없습니다.
        // 실서비스에선 "블랙리스트" 저장소를 운용하거나, 클라이언트가 토큰을 삭제하도록 안내합니다.
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }
}


