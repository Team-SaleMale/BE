package com.salemale.global.security.jwt; // 매 요청마다 Authorization 헤더의 JWT를 검증하는 필터

import jakarta.servlet.FilterChain; // 필터 체인 제어
import jakarta.servlet.ServletException; // 서블릿 예외
import jakarta.servlet.http.HttpServletRequest; // 요청 객체
import jakarta.servlet.http.HttpServletResponse; // 응답 객체
import org.springframework.http.HttpHeaders; // 표준 헤더 상수(Authorization 등)
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // 인증 객체 표현
import org.springframework.security.core.context.SecurityContextHolder; // 현재 스레드의 보안 컨텍스트
import org.springframework.security.core.userdetails.User; // 간단한 UserDetails 구현
import org.springframework.security.core.userdetails.UserDetails; // 인증 주체 표현 인터페이스
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; // 요청 세부정보 바인딩
import org.springframework.util.StringUtils; // 문자열 유틸
import org.springframework.web.filter.OncePerRequestFilter; // 요청당 1회 실행 보장 필터

import java.io.IOException; // IO 예외
import java.util.Collections; // 빈 권한 컬렉션

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider; // 토큰 파싱/검증기

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Authorization: Bearer <token> 형태의 헤더에서 토큰 추출
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            try {
                // 토큰 유효성/서명 검증 + subject 추출 (subject = 사용자 ID)
                String subject = jwtTokenProvider.getSubject(token);
                // 간단한 인증 주체 생성 (username에 사용자 ID를 넣음, 권한은 추후 확장 가능)
                UserDetails principal = User.withUsername(subject).password("").authorities(Collections.emptyList()).build();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
                // 요청 메타데이터(IP, 세션 ID 등) 부착
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // 현재 스레드의 SecurityContext에 인증 저장 → 이후 컨트롤러에서 @AuthenticationPrincipal 사용 가능
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                // 토큰 파싱/검증 실패 시 인증정보를 비움(익명 사용자로 처리)
                SecurityContextHolder.clearContext();
            }
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}


