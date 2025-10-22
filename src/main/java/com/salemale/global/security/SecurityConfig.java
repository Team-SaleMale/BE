package com.salemale.global.security; // 스프링 시큐리티 전역 설정

import org.springframework.context.annotation.Bean; // @Bean 등록
import org.springframework.context.annotation.Configuration; // 설정 클래스
import org.springframework.security.config.Customizer; // 기본 httpBasic 설정 등
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // 보안 DSL
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // 시큐리티 활성화
import org.springframework.security.config.http.SessionCreationPolicy; // 세션 정책(STATeless)
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCrypt 구현체
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 인코더 인터페이스
import org.springframework.security.web.SecurityFilterChain; // 필터 체인 빈
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // 커스텀 필터 삽입 지점
import com.salemale.global.security.jwt.JwtAuthenticationFilter; // JWT 인증 필터
import com.salemale.global.security.jwt.JwtTokenProvider; // 토큰 프로바이더
import org.springframework.web.cors.CorsConfiguration; // CORS 정책 정의
import org.springframework.web.cors.CorsConfigurationSource; // CORS 설정 소스
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // URL 패턴별 CORS 적용

import java.util.Arrays; // 허용 메서드/헤더 나열에 사용
import java.util.List; // 허용 오리진 목록에 사용

@Configuration // 스프링 구성 클래스
@EnableWebSecurity // 웹 보안 활성화
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider; // JWT 파서/생성기 주입
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;  // 추가

    // 생성자 수정
    public SecurityConfig(JwtTokenProvider jwtTokenProvider, CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;  // 추가
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 활성화 + 커스텀 소스 적용
                .csrf(csrf -> csrf.disable()) // REST + JWT 환경에서 CSRF 보호 비활성화(상태 비저장)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 미사용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", // 루트
                                "/swagger-ui.html", // 스웨거 UI
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/actuator/health", // 헬스체크
                                "/auth/**",
                                "/api/auth/**" // 로그인/회원가입/로그아웃 등 인증 경로는 공개(과거 프리픽스 호환)
                        ).permitAll()
                        .anyRequest().authenticated() // 그 외는 인증 필요
                )
                // 로그인 안하고 api 진행했을때 생성되는 에러 응답 추가
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .httpBasic(Customizer.withDefaults()); // httpBasic 기본값(사용 안 해도 무방)

        // UsernamePasswordAuthenticationFilter 전에 JWT 인증 필터를 등록
        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

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
                "https://valuebid.site", // 백엔드 배포 서버
                "http://localhost:3000", // React/Next.js 로컬 개발
                "http://127.0.0.1:3000", // React/Next.js 루프백
                "http://localhost:5173", // Vite/Vue 로컬 개발
                "http://127.0.0.1:5173", // Vite/Vue 루프백
                "http://localhost:8080", // 로컬(호스트네임 - 백엔드/스웨거)
                "http://127.0.0.1:8080" // 로컬(루프백 IP - 백엔드)
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")); // 허용 메서드
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With")); // 허용 헤더
        configuration.setExposedHeaders(List.of("Authorization")); // 클라이언트에서 읽을 수 있는 응답 헤더
        configuration.setAllowCredentials(true); // 인증정보(쿠키/Authorization) 포함 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 전 경로에 위 정책 적용
        return source;
    }
}


