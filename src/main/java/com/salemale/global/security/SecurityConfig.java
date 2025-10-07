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

@Configuration // 스프링 구성 클래스
@EnableWebSecurity // 웹 보안 활성화
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider; // JWT 파서/생성기 주입

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                                "/auth/**" // 로그인/회원가입/로그아웃 등 인증 경로는 공개
                        ).permitAll()
                        .anyRequest().authenticated() // 그 외는 인증 필요
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
}


