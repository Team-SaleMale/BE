package com.salemale.global.security.oauth;

import com.salemale.domain.auth.service.SocialSignupSessionService;
import com.salemale.domain.user.repository.UserAuthRepository;
import com.salemale.global.common.enums.LoginType;
import com.salemale.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * OAuth2 인증 성공 시 호출되는 핸들러
 * 
 * - 카카오/네이버 로그인 성공 시 사용자 정보를 DB에 저장/업데이트하고
 * - JWT 토큰을 발급하여 URL fragment(#token=...)로 프론트엔드에 전달합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final SocialSignupSessionService socialSignupSessionService;
    private final UserAuthRepository userAuthRepository;
    
    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        
        // 인증 제공자 정보 추출 (kakao 또는 naver)
        String registrationId = getRegistrationId(request);
        LoginType loginType = determineLoginType(registrationId);
        
        log.info("OAuth2 로그인 성공: provider={}", loginType);
        
        // 사용자 정보 파싱
        Map<String, Object> attributes = oauth2User.getAttributes();
        String providerUserId = extractProviderUserId(oauth2User.getName(), attributes, loginType);
        String email = extractEmail(attributes, loginType);
        String nickname = extractNickname(attributes, loginType);
        
        log.debug("파싱된 사용자 정보: provider={}, providerUserId={}, email={}, nickname={}", 
                  loginType, providerUserId, email, nickname);
        
        // 기존 회원 여부 확인 (provider + providerUserId로 조회)
        var existingAuth = userAuthRepository.findByProviderAndProviderUserId(loginType, providerUserId);
        
        if (existingAuth.isPresent()) {
            // 기존 회원: JWT 토큰 발급 후 바로 로그인 처리
            var userAuth = existingAuth.get();
            var user = userAuth.getUser();
            
            // 소프트 삭제 계정은 로그인 불가
            if (user.getDeletedAt() != null) {
                log.warn("소프트 삭제된 계정 로그인 시도: userId={}", user.getId());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "삭제된 계정입니다.");
                return;
            }
            
            // 최근 로그인 시간 업데이트
            userAuth.updateLastLoginAt(LocalDateTime.now());
            userAuthRepository.save(userAuth);
            
            // JWT 토큰 생성
            String userId = String.valueOf(user.getId());
            String accessToken = jwtTokenProvider.generateToken(userId);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
            
            // 리프레시 토큰을 쿠키에 저장
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .maxAge(Duration.ofDays(14))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
            
            String baseUri = getRedirectUri();
            String redirectUri = baseUri + "#token=" + accessToken;
            log.info("OAuth2 로그인 성공 (기존 회원), 리다이렉트: {}#token=***", baseUri);
            getRedirectStrategy().sendRedirect(request, response, redirectUri);
        } else {
            // 신규 회원: 가입 확정 세션 발급 → 프론트에서 닉네임/지역 입력 후 완료 호출
            String signupToken = socialSignupSessionService.create(loginType, providerUserId, email);
            String baseUri = getRedirectUri();
            String redirectUri = baseUri + "#social=1&provider=" + registrationId + "&signupToken=" + signupToken;
            log.info("OAuth2 인증 완료(신규 회원, 미확정), 리다이렉트: {}#social=1&provider={}&signupToken=***", baseUri, registrationId);
            getRedirectStrategy().sendRedirect(request, response, redirectUri);
        }
    }
    
    /**
     * 요청에서 registrationId 추출 (kakao 또는 naver)
     */
    private String getRegistrationId(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/oauth2/authorization/kakao") || requestURI.contains("/login/oauth2/code/kakao")) {
            return "kakao";
        } else if (requestURI.contains("/oauth2/authorization/naver") || requestURI.contains("/login/oauth2/code/naver")) {
            return "naver";
        }
        throw new IllegalArgumentException("알 수 없는 OAuth2 제공자");
    }
    
    /**
     * registrationId를 LoginType enum으로 변환
     */
    private LoginType determineLoginType(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> LoginType.KAKAO;
            case "naver" -> LoginType.NAVER;
            default -> throw new IllegalArgumentException("지원하지 않는 제공자: " + registrationId);
        };
    }
    
    /**
     * 제공자별 사용자 ID 추출
     */
    private String extractProviderUserId(String name, Map<String, Object> attributes, LoginType loginType) {
        return switch (loginType) {
            case KAKAO -> String.valueOf(attributes.get("id"));
            case NAVER -> {
                Object resp = attributes.get("response");
                if (resp instanceof java.util.Map<?, ?> map && map.get("id") != null) {
                    yield String.valueOf(map.get("id"));
                }
                yield null;
            }
            default -> name;
        };
    }
    
    /**
     * 제공자별 이메일 추출
     */
    private String extractEmail(Map<String, Object> attributes, LoginType loginType) {
        return switch (loginType) {
            case KAKAO -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null && kakaoAccount.containsKey("email")) {
                    yield (String) kakaoAccount.get("email");
                }
                yield null;
            }
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                if (response != null) {
                    yield (String) response.get("email");
                }
                yield null;
            }
            default -> null;
        };
    }
    
    /**
     * 제공자별 닉네임 추출
     */
    private String extractNickname(Map<String, Object> attributes, LoginType loginType) {
        return switch (loginType) {
            case KAKAO -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    if (profile != null && profile.containsKey("nickname")) {
                        yield (String) profile.get("nickname");
                    }
                }
                // 닉네임이 없으면 기본값 설정
                yield "카카오사용자";
            }
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                if (response != null && response.containsKey("nickname")) {
                    yield (String) response.get("nickname");
                }
                // 닉네임이 없으면 이메일 기반 생성
                String email = extractEmail(attributes, loginType);
                if (email != null) {
                    int atIndex = email.indexOf('@');
                    String prefix = atIndex > 0 ? email.substring(0, atIndex) : email;
                    yield "네이버_" + prefix;
                }
                yield "네이버사용자";
            }
            default -> "사용자";
        };
    }
    
    // 기존 즉시 생성 로직은 제거. 최종 확정은 컨트롤러의 /auth/social/complete에서 수행
    
    /**
     * 리다이렉트 URI (프론트엔드 URL)
     * 환경변수 FRONTEND_URL을 사용하고, 없으면 기본값 사용
     */
    private String getRedirectUri() {
        return frontendUrl + "/auth/callback";
    }
}

