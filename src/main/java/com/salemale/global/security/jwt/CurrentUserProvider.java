package com.salemale.global.security.jwt; // JWT에서 현재 사용자 ID 추출 유틸리티

import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    // 원리: Authorization: Bearer <token> → subject 추출
    // 1) subject가 숫자면 userId로 간주
    // 2) 숫자가 아니면 이메일로 간주하고 UserRepository에서 조회해 id 반환
    public Long getCurrentUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or invalid Authorization header");
        }
        String token = header.substring(7);
        String subject = jwtTokenProvider.getSubject(token);
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            User user = userRepository.findByEmail(subject)
                    .orElseThrow(() -> new IllegalStateException("User not found by email subject: " + subject));
            return user.getId();
        }
    }
}


