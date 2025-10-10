package com.salemale.global.security.jwt; // JWT에서 현재 사용자 ID 추출 유틸리티

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * HTTP 요청에서 JWT 토큰을 추출하여 현재 사용자 ID를 반환합니다.
     * 
     * 원리:
     * 1) Authorization: Bearer <token> 헤더에서 토큰 추출
     * 2) JWT subject에서 userId를 추출하여 반환
     * 
     * @param request HTTP 요청 객체
     * @return 현재 사용자 ID
     * @throws AuthenticationCredentialsNotFoundException 토큰이 없거나 유효하지 않을 때
     * @throws NumberFormatException subject가 유효한 숫자가 아닐 때
     */
    public Long getCurrentUserId(HttpServletRequest request) {
        // 1) Authorization 헤더 추출 (case-insensitive)
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Missing Authorization header");
        }
        
        // 2) Bearer 스키마 확인 (case-insensitive)
        String trimmedHeader = header.trim();
        if (!trimmedHeader.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            throw new AuthenticationCredentialsNotFoundException("Invalid Authorization header format");
        }
        
        // 3) 토큰 추출 및 검증
        String token = trimmedHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Empty JWT token");
        }
        
        // 4) Subject 추출 (userId)
        String subject = jwtTokenProvider.getSubject(token);
        
        // 5) Subject를 userId로 변환
        return Long.parseLong(subject);
    }
}


