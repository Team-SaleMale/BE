package com.salemale.domain.auth.controller;

import com.salemale.common.response.ApiResponse;
import com.salemale.domain.auth.dto.request.LoginRequest;
import com.salemale.domain.auth.dto.request.SignupRequest;
import com.salemale.domain.auth.dto.request.PasswordResetRequest;
import com.salemale.domain.auth.dto.request.PasswordResetVerifyRequest;
import com.salemale.domain.auth.dto.request.PasswordResetConfirmRequest;
import com.salemale.domain.auth.service.AuthService;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.entity.UserAuth;
import com.salemale.domain.user.repository.UserAuthRepository;
import com.salemale.domain.user.repository.UserRepository;
import com.salemale.global.common.enums.LoginType;
import com.salemale.global.security.jwt.CurrentUserProvider;
import com.salemale.domain.auth.service.SignupVerificationService;
import com.salemale.domain.auth.service.SocialSignupSessionService;
import com.salemale.domain.region.entity.Region;
import com.salemale.domain.region.repository.RegionRepository;
import com.salemale.domain.user.entity.UserRegion;
import com.salemale.domain.user.repository.UserRegionRepository;
import com.salemale.domain.auth.service.PasswordResetService;
import com.salemale.global.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final SignupVerificationService signupVerificationService;
    private final SocialSignupSessionService socialSignupSessionService;
    private final RegionRepository regionRepository;
    private final UserRegionRepository userRegionRepository;

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider, PasswordResetService passwordResetService,
                          CurrentUserProvider currentUserProvider, UserRepository userRepository,
                          UserAuthRepository userAuthRepository,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          SignupVerificationService signupVerificationService,
                          SocialSignupSessionService socialSignupSessionService,
                          RegionRepository regionRepository,
                          UserRegionRepository userRegionRepository) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordResetService = passwordResetService;
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
        this.signupVerificationService = signupVerificationService;
        this.socialSignupSessionService = socialSignupSessionService;
        this.regionRepository = regionRepository;
        this.userRegionRepository = userRegionRepository;
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OAuth2 로그인 안내 정보 제공")
    })
    @GetMapping("/oauth2/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> oauth2LoginInfo() {
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of(
                "kakao", "GET /oauth2/authorization/kakao - 카카오 로그인",
                "naver", "GET /oauth2/authorization/naver - 네이버 로그인",
                "callback", frontendUrl + "/auth/callback#token={JWT_TOKEN}",
                "note", "브라우저에서 직접 접속해야 합니다. Swagger UI에서는 테스트할 수 없습니다."
        )));
    }

    @Operation(
            summary = "회원가입 이메일 인증코드 발송",
            description = "로컬 회원가입 전 이메일로 6자리 인증코드를 보냅니다. 기존 계정 존재 여부는 응답에서 노출하지 않습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 요청 접수")
    })
    @PostMapping("/email/verify/request")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestSignupEmail(@RequestParam("email") String email) {
        // 회원가입 정책: 미가입 이메일만 허용
        if (authService.existsLocalEmail(email)) {
            return ResponseEntity.status(400).body(ApiResponse.onFailure("USER_EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.", null));
        }
        signupVerificationService.sendSignupCode(email);
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of("message", "인증코드가 이메일로 발송되었습니다.")));
    }

    @Operation(
            summary = "회원가입 이메일 인증코드 검증",
            description = "이메일과 6자리 코드를 검증하여 단기 세션 토큰을 반환합니다. 이후 회원가입 요청 시 제출하세요."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검증 성공, 세션 토큰 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "코드가 유효하지 않음/만료됨")
    })
    @PostMapping("/email/verify/confirm")
    public ResponseEntity<ApiResponse<Map<String, String>>> confirmSignupEmail(
            @RequestParam("email") String email,
            @RequestParam("code") String code
    ) {
        String sessionToken = signupVerificationService.verifySignupCode(email, code);
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of("sessionToken", sessionToken)));
    }

    @Operation(
            summary = "계정 탈퇴(소프트 삭제)",
            description = "현재 로그인한 사용자의 계정을 소프트 삭제합니다. 로컬 계정은 비밀번호 확인이 필요합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 또는 비밀번호 불일치")
    })
    @DeleteMapping("/me")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Void>> deleteMe(
            jakarta.servlet.http.HttpServletRequest request,
            @Parameter(description = "로컬 계정일 경우 비밀번호", required = false)
            @RequestParam(name = "password", required = false) String password
    ) {
        Long userId = currentUserProvider.getCurrentUserId(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for authenticated userId: " + userId));

        // 로컬 계정은 비밀번호 확인
        UserAuth localAuth = userAuthRepository.findByProviderAndUser(LoginType.LOCAL, user).orElse(null);
        if (localAuth != null) {
            if (password == null || password.isBlank()) {
                var err = com.salemale.common.code.status.ErrorStatus.MISSING_PASSWORD;
                return ResponseEntity.status(err.getHttpStatus())
                        .body(ApiResponse.onFailure(err.getCode(), err.getMessage(), null));
            }
            if (localAuth.getPasswordHash() == null || !passwordEncoder.matches(password, localAuth.getPasswordHash())) {
                var err = com.salemale.common.code.status.ErrorStatus.AUTH_INVALID_CREDENTIALS;
                return ResponseEntity.status(err.getHttpStatus())
                        .body(ApiResponse.onFailure(err.getCode(), err.getMessage(), null));
            }
        }

        user.markDeletedNow();
        userRepository.save(user);

        // 연결된 UserAuth도 소프트 삭제 처리
        for (UserAuth ua : userAuthRepository.findAllByUser(user)) {
            ua.markDeletedNow();
            userAuthRepository.save(ua);
        }

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
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody SignupRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Email-Verify-Token", required = false) String token
    ) {
        // 이메일 인증 토큰 필수: 미제공/검증실패 시 가입 거부
        if (token == null || !signupVerificationService.validateSignupToken(request.getEmail(), token)) {
            return ResponseEntity.status(400).body(ApiResponse.onFailure("EMAIL_VERIFICATION_REQUIRED", "이메일 인증을 완료해주세요.", null));
        }

        // 닉네임/지역 필수 및 중복 검사
        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            var err = com.salemale.common.code.status.ErrorStatus.NICKNAME_NOT_EXIST;
            return ResponseEntity.status(err.getHttpStatus())
                    .body(ApiResponse.onFailure(err.getCode(), err.getMessage(), null));
        }
        if (authService.existsNickname(request.getNickname().trim())) {
            var err = com.salemale.common.code.status.ErrorStatus.NICKNAME_ALREADY_EXISTS;
            return ResponseEntity.status(err.getHttpStatus())
                    .body(ApiResponse.onFailure(err.getCode(), err.getMessage(), null));
        }
        if (request.getRegionId() == null) {
            var err = com.salemale.common.code.status.ErrorStatus.REGION_NOT_FOUND;
            return ResponseEntity.status(err.getHttpStatus())
                    .body(ApiResponse.onFailure(err.getCode(), err.getMessage(), null));
        }

        authService.registerLocal(request);
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    @Operation(summary = "소셜 회원가입 최종 확정", description = "OAuth2 성공 후 signupToken, 닉네임, 지역을 받아 User/UserRegion/UserAuth를 생성합니다.")
    @PostMapping("/social/complete")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Void>> completeSocialSignup(
            @RequestParam("signupToken") String signupToken,
            @RequestParam("nickname") String nickname,
            @RequestParam("regionId") Long regionId
    ) {
        var session = socialSignupSessionService.get(signupToken);
        if (session == null) {
            var err = com.salemale.common.code.status.ErrorStatus.SOCIAL_SIGNUP_SESSION_INVALID;
            return ResponseEntity.status(err.getHttpStatus())
                    .body(ApiResponse.onFailure(err.getCode(), err.getMessage(), null));
        }

        // 닉네임 검증
        if (nickname == null || nickname.trim().isEmpty()) {
            var err = com.salemale.common.code.status.ErrorStatus.NICKNAME_NOT_EXIST;
            return ResponseEntity.status(err.getHttpStatus())
                    .body(ApiResponse.onFailure(err.getCode(), err.getMessage(), null));
        }
        if (authService.existsNickname(nickname.trim())) {
            var err = com.salemale.common.code.status.ErrorStatus.NICKNAME_ALREADY_EXISTS;
            return ResponseEntity.status(err.getHttpStatus())
                    .body(ApiResponse.onFailure(err.getCode(), err.getMessage(), null));
        }
        if (regionId == null) {
            var err = com.salemale.common.code.status.ErrorStatus.REGION_NOT_FOUND;
            return ResponseEntity.status(err.getHttpStatus())
                    .body(ApiResponse.onFailure(err.getCode(), err.getMessage(), null));
        }

        // 서비스로 일원화하여 생성 (소셜 회원가입은 이메일 없음)
        authService.completeSocialSignup(
                nickname,
                regionId,
                session.provider(),
                session.providerUserId()
        );

        socialSignupSessionService.consume(signupToken);
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

