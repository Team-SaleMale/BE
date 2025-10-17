package com.salemale.domain.user.controller; // 인증 HTTP 엔드포인트 정의(로그인/회원가입/로그아웃 등)

import com.salemale.common.response.ApiResponse; // 통일된 API 응답 포맷 래퍼
import com.salemale.domain.user.dto.request.LoginRequest; // 로그인 요청 DTO(email/password)
import com.salemale.domain.user.dto.request.SignupRequest; // 회원가입 요청 DTO(email/nickname/password)
import com.salemale.domain.user.service.AuthService; // 인증 비즈니스 로직 서비스(로그인/회원가입 등)
import com.salemale.global.security.jwt.JwtTokenProvider; // JWT 생성/검증기
import io.swagger.v3.oas.annotations.Operation; // Swagger: API 설명
import io.swagger.v3.oas.annotations.Parameter; // Swagger: 파라미터 설명
import io.swagger.v3.oas.annotations.responses.ApiResponses; // Swagger: 여러 응답 설명
import io.swagger.v3.oas.annotations.tags.Tag; // Swagger: 컨트롤러 그룹 태그
import jakarta.validation.Valid; // 요청 바디 검증
import org.springframework.http.ResponseCookie; // 쿠키 작성 유틸
import org.springframework.http.ResponseEntity; // HTTP 응답 래퍼
import org.springframework.http.HttpHeaders; // 헤더 상수
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑
import org.springframework.web.bind.annotation.PatchMapping; // PATCH 매핑(로그아웃 등 상태변경용)
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑(상태 점검)
import org.springframework.web.bind.annotation.CookieValue; // 쿠키 값 바인딩
import org.springframework.web.bind.annotation.RequestParam; // 쿼리 파라미터 바인딩
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 인증된 사용자 주입
import org.springframework.security.core.userdetails.UserDetails; // 인증 주체 표현
import org.springframework.web.bind.annotation.RequestBody; // 요청 바디 바인딩
import org.springframework.web.bind.annotation.RequestMapping; // 베이스 경로 매핑
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러 선언

import java.time.Duration; // 쿠키 만료 설정
import java.util.Map; // 간단한 키/값 응답을 위해 사용

@RestController
@RequestMapping("/auth") // 모든 인증 관련 경로는 /auth 하위로 통일(/auth/login, /auth/register, /auth/logout)
@Tag(name = "인증", description = "로그인, 회원가입, 로그아웃, 이메일/닉네임 중복 체크 API")
public class AuthController { // 인증 관련 엔드포인트 집합(초심자도 이해할 수 있도록 상세 주석 포함)

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) { // 생성자를 통해 서비스 의존성 주입
        this.authService = authService; // 스프링이 AuthService 빈을 자동으로 넣어줍니다.
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Operation(
            summary = "로그인",
            description = "이메일/비밀번호로 로그인하고 JWT 액세스 토큰을 발급받습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 발급"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    @PostMapping("/login") // 로그인: 사용자가 이메일/비밀번호를 보내면 서버가 확인 후 토큰을 발급해 줍니다.
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
        // 1) @Valid: request에 적힌 @Email, @NotBlank 등의 검사를 먼저 수행합니다.
        // 2) 서비스에 로그인 요청: 비밀번호 해시 검증에 성공하면 JWT 토큰이 문자열로 돌아옵니다.
        String token = authService.loginLocal(request.getEmail(), request.getPassword());
        // refresh 토큰 생성(회전 전략은 추후 저장소 도입 시 강화)
        String subject = jwtTokenProvider.getSubject(token);
        String refresh = jwtTokenProvider.generateRefreshToken(subject);

        // HttpOnly + Secure + SameSite=None 쿠키로 전달(크로스 도메인 사용을 위해)
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();
        // 3) ApiResponse.onSuccess로 성공 응답을 표준 형태로 감싸서 반환합니다.
        //    응답 본문 예시: { "isSuccess": true, "code": "200", "message": "OK", "result": { "accessToken": "..." } }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.onSuccess(Map.of("accessToken", token)));
    }

    @Operation(
            summary = "회원가입",
            description = "이메일/닉네임/비밀번호로 새 계정을 생성합니다. 비밀번호는 안전하게 해시되어 저장됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 가입된 이메일 또는 유효하지 않은 입력")
    })
    @PostMapping("/register") // 회원가입: 새 사용자를 만들고 로컬 자격(이메일/비번)을 저장합니다.
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody SignupRequest request) {
        // 1) 이메일/닉네임/비밀번호 입력 검증(@Valid)
        // 2) 서비스에 위임: 중복 이메일 검사 → User 생성 → UserAuth(LOCAL) 생성(비밀번호 해시)
        authService.registerLocal(request);
        // 3) 생성 성공 시 바디 없이 성공 응답을 반환합니다.
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    @Operation(
            summary = "로그아웃",
            description = "로그아웃합니다. JWT 토큰은 클라이언트에서 삭제해야 합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PatchMapping("/logout") // 로그아웃: 리프레시 쿠키 제거
    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie delete = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, delete.toString())
                .body(ApiResponse.onSuccess());
    }

    @Operation(
            summary = "토큰 재발급",
            description = "HttpOnly 쿠키의 리프레시 토큰으로 액세스 토큰을 재발급합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "리프레시 토큰 유효하지 않음")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("COMMON401", "리프레시 토큰이 없습니다.", null));
        }

        try {
            // 유효성 + 타입(refresh) 검증 후 subject 추출
            String subject = jwtTokenProvider.getSubjectIfTokenType(refreshToken, "refresh");

            // 새 액세스/리프레시 발급(회전)
            String newAccess = jwtTokenProvider.generateToken(subject);
            String newRefresh = jwtTokenProvider.generateRefreshToken(subject);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefresh)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(Duration.ofDays(14))
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(ApiResponse.onSuccess(Map.of("accessToken", newAccess)));
        } catch (io.jsonwebtoken.JwtException ex) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("COMMON401", "유효하지 않은 리프레시 토큰입니다.", null));
        }
    }

    @Operation(
            summary = "이메일 중복 체크",
            description = "입력한 이메일이 이미 가입되어 있는지 확인합니다. 회원가입 전 중복 검사용입니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "중복 검사 완료")
    })
    @GetMapping("/check/email") // 이메일(로그인 ID) 중복 체크: true/false로 빠르게 응답
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(
            @Parameter(description = "확인할 이메일 주소", example = "user@example.com")
            @RequestParam("value") String email) {
        // 보안: 계정 열거(account enumeration) 완화
        // - 실서비스에선 반드시 IP/디바이스 기준 레이트리밋(예: 분당 N회)을 적용하세요.
        // - CAPTCHA나 가입 플로우 내부에서만 사용하도록 제한하는 것도 효과적입니다.
        // - 이 데모는 레이트리밋 미구현 상태이므로, 운영 전 게이트 추가를 권장합니다.

        String normalized = email.trim().toLowerCase(); // 입력 정규화
        boolean exists = authService.existsLocalEmail(normalized);
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of("exists", exists)));
    }

    @Operation(
            summary = "닉네임 중복 체크",
            description = "입력한 닉네임이 이미 사용 중인지 확인합니다. 회원가입 전 중복 검사용입니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "중복 검사 완료")
    })
    @GetMapping("/check/nickname") // 닉네임 중복 체크: true/false 응답
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(
            @Parameter(description = "확인할 닉네임", example = "홍길동")
            @RequestParam("value") String nickname) {
        // 1) 닉네임은 표시용이므로 그대로 검사(정책에 따라 trim/소문자화 가능)
        boolean exists = authService.existsNickname(nickname);
        // 2) {"exists": true/false}
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of("exists", exists)));
    }

    @Operation(
            summary = "로그인 상태 확인",
            description = "JWT 토큰이 유효한지 확인하고 현재 로그인한 사용자 정보(사용자 ID)를 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 토큰 없음 또는 유효하지 않음)")
    })
    @GetMapping("/me") // 로그인 상태 확인: 토큰이 유효하면 주체(subject: 사용자 ID)를 반환, 아니면 401
    public ResponseEntity<ApiResponse<Map<String, String>>> me(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        // 1) JwtAuthenticationFilter가 토큰을 검증하고 SecurityContext에 주체(UID)를 세팅합니다.
        // 2) @AuthenticationPrincipal로 인증된 사용자 정보를 주입받을 수 있습니다.
        // 3) principal.getUsername()은 JWT의 subject(사용자 ID)입니다.
        if (principal == null) {
            // 스프링 시큐리티가 자동으로 401을 보내도록 구성되어 있으나, 명시적으로 실패 응답을 줄 수도 있습니다.
            return ResponseEntity.status(401)
                    .body(ApiResponse.onFailure("COMMON401", "인증이 필요합니다.", null));
        }
        // subject = 사용자 ID (숫자 문자열)
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of("userId", principal.getUsername())));
    }
}


