package com.salemale.global.security; // 스프링 시큐리티 전역 설정

import org.springframework.context.annotation.Bean; // @Bean 등록
import org.springframework.context.annotation.Configuration; // 설정 클래스
import org.springframework.security.config.Customizer; // 기본 httpBasic 설정 등
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // 보안 DSL
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // 시큐리티 활성화
import org.springframework.security.config.http.SessionCreationPolicy; // 세션 정책(STATeless)
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCrypt 구현체
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 인코더 인터페이스
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain; // 필터 체인 빈
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // 커스텀 필터 삽입 지점
import com.salemale.global.security.jwt.JwtAuthenticationFilter; // JWT 인증 필터
import com.salemale.global.security.jwt.JwtTokenProvider; // 토큰 프로바이더
import com.salemale.global.security.oauth.OAuth2AuthenticationSuccessHandler; // OAuth2 성공 핸들러
import com.salemale.domain.user.repository.UserRepository; // 삭제 계정 확인용
import org.springframework.web.cors.CorsConfiguration; // CORS 정책 정의
import org.springframework.web.cors.CorsConfigurationSource; // CORS 설정 소스
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // URL 패턴별 CORS 적용
import jakarta.servlet.http.HttpServletResponse; // 응답 객체

import java.util.HashMap;
import java.util.Arrays; // 허용 메서드/헤더 나열에 사용
import java.util.List; // 허용 오리진 목록에 사용
import java.util.Map;

@Configuration // 스프링 구성 클래스
@EnableWebSecurity // 웹 보안 활성화
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider; // JWT 파서/생성기 주입
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint; // 인증 실패 엔트리포인트
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler; // OAuth2 성공 핸들러
    private final UserRepository userRepository; // JWT 필터 주입

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, 
                         CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                         OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                         UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 활성화 + 커스텀 소스 적용
                .csrf(csrf -> csrf.disable()) // REST + JWT 환경에서 CSRF 보호 비활성화(상태 비저장)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 미사용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/ws-stomp/**",  // WS 추가
                                "/ws-stomp",     // WS 추가
                                "/stomp-test.html", // WS 추가(테스트 끝나면 삭제)
                                "/", // 루트
                                "/swagger-ui.html", // 스웨거 UI
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/actuator/health", // 헬스체크
                                "/error", // Spring Boot 기본 에러 엔드포인트
                                "/auth/**",
                                "/api/auth/**", // 로그인/회원가입/로그아웃 등 인증 경로는 공개(과거 프리픽스 호환)
                                "/oauth2/authorization/**", // OAuth2 인증 시작 경로
                                "/login/oauth2/code/**", // OAuth2 콜백 경로
                                "/search/regions", // 지역 검색 API (인증 불필요)
                                "/search/price-history", // 중고 시세 검색 API (인증 불필요)
                                "/auctions", // 경매 상품 리스트 조회 (인증 선택적: RECOMMENDED 제외하고는 불필요)
                                "/auctions/**" // 경매 상품 상세 조회 (인증 선택적: 공개 정보)
                        ).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight 요청 허용
                        .anyRequest().authenticated() // 그 외는 인증 필요
                )
                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService()))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            // OAuth2 실패 시 로그
                            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                                    .error("OAuth2 로그인 실패: {}", exception.getMessage(), exception);
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 로그인 실패");
                        })
                )
                // 로그인 안하고 api 진행했을때 생성되는 에러 응답 추가
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .httpBasic(Customizer.withDefaults()); // httpBasic 기본값(사용 안 해도 무방)

        // UsernamePasswordAuthenticationFilter 전에 JWT 인증 필터를 등록
        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build(); // 필터 체인 빌드
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt로 비밀번호 해시/검증
    }

    // CORS 정책 정의: 프론트/로컬 개발 오리진만 허용
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "https://valuebid.netlify.app", // 배포된 프론트엔드
                "https://valuebid.site", // 배포된 백엔드
                "http://localhost:3000", // React/Next.js 로컬 개발
                "http://127.0.0.1:3000", // React/Next.js 루프백
                "http://localhost:5173", // Vite/Vue 로컬 개발
                "http://127.0.0.1:5173", // Vite/Vue 루프백
                "http://localhost:8080", // 로컬(호스트네임 - 백엔드/스웨거)
                "http://127.0.0.1:8080" // 로컬(루프백 IP - 백엔드)
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")); // 허용 메서드
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Email-Verify-Token", "USER-ID", "user-id", "Accept")); // 허용 헤더 (CORS preflight용, 대소문자 모두 허용)
        configuration.setExposedHeaders(List.of("Authorization")); // 클라이언트에서 읽을 수 있는 응답 헤더
        configuration.setAllowCredentials(true); // 인증정보(쿠키/Authorization) 포함 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 전 경로에 위 정책 적용
        return source;
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return new FlatteningOAuth2UserService();
    }

    private static class FlatteningOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

        private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
            OAuth2User oAuth2User = delegate.loadUser(userRequest);
            Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
            String registrationId = userRequest.getClientRegistration().getRegistrationId();

            if ("naver".equalsIgnoreCase(registrationId)) {
                Map<String, Object> responseAttributes = extractResponse(attributes);
                attributes.put("id", responseAttributes.get("id"));
                attributes.put("email", responseAttributes.get("email"));
                attributes.put("nickname", responseAttributes.get("nickname"));
                attributes.put("name", responseAttributes.getOrDefault("nickname", responseAttributes.get("name")));
                return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, "id");
            }

            String userNameAttributeName = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();

            return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, userNameAttributeName);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> extractResponse(Map<String, Object> attributes) {
            Object response = attributes.get("response");
            if (response instanceof Map) {
                return (Map<String, Object>) response;
            }
            return Map.of();
        }
    }
}


