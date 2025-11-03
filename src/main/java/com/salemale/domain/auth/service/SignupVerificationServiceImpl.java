package com.salemale.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 회원가입 이메일 인증 서비스의 Redis 구현체.
 * - signup:code:{email} 키에 6자리 코드와 TTL 저장
 * - verify 성공 시 signup:token:{token} 키에 email과 TTL 저장
 * - 토큰은 회원가입 API 헤더(X-Email-Verify-Token)로 제출되어 검증됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignupVerificationServiceImpl implements SignupVerificationService {

    private final EmailService emailService;
    private final StringRedisTemplate stringRedisTemplate;

    /** 인증코드 만료시간(초단기) */
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    /** 세션토큰 만료시간(회원가입 완료까지 유지) */
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private static final String KEY_CODE = "signup:code:";     // signup:code:{email}
    private static final String KEY_TOKEN = "signup:token:";    // signup:token:{token}

    private final SecureRandom random = new SecureRandom();

    @Override
    public void sendSignupCode(String email) {
        String normalized = email.toLowerCase();
        String code = String.format("%06d", random.nextInt(1_000_000));

        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set(KEY_CODE + normalized, code, CODE_TTL);

        // 가입 전용 템플릿 사용
        emailService.sendSignupVerificationCode(email, code);
        log.info("[signup] code issued to {} (ttl={}s)", mask(email), CODE_TTL.toSeconds());
    }

    @Override
    public String verifySignupCode(String email, String code) {
        String normalized = email.toLowerCase();
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String key = KEY_CODE + normalized;
        String stored = ops.get(key);
        if (stored == null || !stored.equals(code)) {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }
        // 일회용: 즉시 삭제
        stringRedisTemplate.delete(key);

        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes());
        ops.set(KEY_TOKEN + token, normalized, TOKEN_TTL);
        return token;
    }

    @Override
    public boolean validateSignupToken(String email, String token) {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String normalized = email.toLowerCase();
        String owner = ops.get(KEY_TOKEN + token);
        return normalized.equals(owner);
    }

    /**
     * 로그에 노출될 이메일을 간단히 마스킹합니다.
     */
    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}


