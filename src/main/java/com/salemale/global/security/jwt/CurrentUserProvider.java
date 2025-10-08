package com.salemale.global.security.jwt; // JWT에서 현재 사용자 ID 추출 유틸리티

import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * HTTP 요청에서 JWT 토큰을 추출하여 현재 사용자 ID를 반환합니다.
     * 
     * 원리:
     * 1) Authorization: Bearer <token> 헤더에서 토큰 추출
     * 2) subject가 숫자면 userId로 간주
     * 3) 숫자가 아니면 이메일로 간주하고 UserRepository에서 조회해 id 반환
     * 
     * @param request HTTP 요청 객체
     * @return 현재 사용자 ID
     * @throws AuthenticationCredentialsNotFoundException 토큰이 없거나 유효하지 않을 때
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때
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
        
        // 4) Subject 추출 (userId 또는 email)
        String subject = jwtTokenProvider.getSubject(token);
        
        // 5) Subject가 숫자면 userId로 간주
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            // 6) 숫자가 아니면 이메일로 간주하고 조회 (정규화: 소문자)
            String normalizedEmail = subject.trim().toLowerCase(Locale.ROOT);
            User user = userRepository.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return user.getId();
        }
    }
}


