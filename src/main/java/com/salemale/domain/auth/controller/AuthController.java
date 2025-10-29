package com.salemale.domain.auth.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.auth.dto.request.LoginRequest;
import com.salemale.domain.auth.dto.request.SignupRequest;
import com.salemale.domain.auth.dto.request.PasswordResetRequest;
import com.salemale.domain.auth.dto.request.PasswordResetVerifyRequest;
import com.salemale.domain.auth.dto.request.PasswordResetConfirmRequest;
import com.salemale.domain.auth.service.AuthService;
import com.salemale.domain.auth.service.PasswordResetService;
import com.salemale.global.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "인증", description = "로그인, 회원가입, 로그아웃, 이메일/닉네임 중복 체크 API")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordResetService = passwordResetService;
    }

    @Operation(
            summary = "OAuth2 로그인 (카카오/네이버)",
            description = """
                    카카오 또는 네이버 계정으로 소셜 로그인합니다.
                    
                    ## 사용 방법
                    1. 브라우저에서 다음 URL로 접속:
                       - 카카오: `GET /oauth2/authorization/kakao`
                       - 네이버: `GET /oauth2/authorization/naver`
                    2. 카카오/네이버 로그인 페이지에서 인증
                    3. 로그인 성공 후 프론트엔드로 리다이렉트됨: `{FRONTEND_URL}/auth/callback#token={JWT_TOKEN}`
                    
                    ## 프론트엔드에서 토큰 추출 방법
                    ```javascript
                    // URL fragment에서 토큰 추출 (보안상 fragment 사용)
                    const hashParams = new URLSearchParams(window.location.hash.substring(1));
                    const token = hashParams.get('token');
                    ```
                    
                    **주의**: 토큰은 URL fragment(`#token=...`)로 전달되므로 서버 로그나 Referer 헤더에 노출되지 않습니다.
                    query parameter(`?token=...`)가 아닙니다.
                    """,
            tags = {"인증"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "카카오/네이버 로그인 페이지로 리다이렉트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "OAuth2 로그인 실패")
    })
    @GetMapping("/oauth2/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> oauth2LoginInfo() {
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of(
                "kakao", "GET /oauth2/authorization/kakao - 카카오 로그인",
                "naver", "GET /oauth2/authorization/naver - 네이버 로그인",
                "callback", "{FRONTEND_URL}/auth/callback#token={JWT_TOKEN}",
                "note", "브라우저에서 직접 접속해야 합니다. Swagger UI에서는 테스트할 수 없습니다."
        )));
    }

    @Operation(
            summary = "로그인",
            description = "이메일/비밀번호로 로그인하고 JWT 액세스 토큰을 발급받습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 발급"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.loginLocal(request.getEmail(), request.getPassword());
        String subject = jwtTokenProvider.getSubject(token);
        String refresh = jwtTokenProvider.generateRefreshToken(subject);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

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
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody SignupRequest request) {
        authService.registerLocal(request);
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    @Operation(
            summary = "로그아웃",
            description = "로그아웃합니다. JWT 토큰은 클라이언트에서 삭제해야 합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PatchMapping("/logout")
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
            String subject = jwtTokenProvider.getSubjectIfTokenType(refreshToken, "refresh");
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
    @GetMapping("/check/email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(
            @Parameter(description = "확인할 이메일 주소", example = "user@example.com")
            @RequestParam("value") String email) {
        String normalized = email.trim().toLowerCase();
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
    @GetMapping("/check/nickname")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(
            @Parameter(description = "확인할 닉네임", example = "홍길동")
            @RequestParam("value") String nickname) {
        boolean exists = authService.existsNickname(nickname);
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
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> me(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.onFailure("COMMON401", "인증이 필요합니다.", null));
        }
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of("userId", principal.getUsername())));
    }

    @Operation(
            summary = "비밀번호 재설정 요청",
            description = "비밀번호 재설정용 6자리 인증번호를 이메일로 전송합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증번호 발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 이메일 주소")
    })
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.onSuccess(
                Map.of("message", "인증번호가 이메일로 발송되었습니다.")
        ));
    }

    @Operation(
            summary = "이메일 인증번호 검증",
            description = "6자리 인증번호를 검증합니다. 성공 시 비밀번호 재설정 페이지로 이동 가능합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 인증번호")
    })
    @PostMapping("/password/reset/verify")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyPasswordResetCode(
            @Valid @RequestBody PasswordResetVerifyRequest request) {
        try {
            String sessionToken = passwordResetService.verifyCode(request.getEmail(), request.getCode());
            return ResponseEntity.ok(ApiResponse.onSuccess(
                    Map.of("sessionToken", sessionToken)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.onFailure("CODE_VERIFICATION_FAILED", e.getMessage(), null));
        }
    }

    @Operation(
            summary = "비밀번호 재설정",
            description = "인증이 완료된 상태에서 새로운 비밀번호로 재설정합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이메일 인증이 완료되지 않음")
    })
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            String sessionToken = authorization.startsWith("Bearer ") 
                    ? authorization.substring(7).trim() 
                    : authorization.trim();
            
            passwordResetService.resetPassword(sessionToken, request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.onSuccess());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.onFailure("PASSWORD_RESET_FAILED", e.getMessage(), null));
        }
    }
}

