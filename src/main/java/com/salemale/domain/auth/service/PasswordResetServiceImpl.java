package com.salemale.domain.auth.service;

import com.salemale.domain.user.entity.UserAuth;
import com.salemale.domain.user.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * PasswordResetServiceImpl: 비밀번호 재설정 서비스 구현체 (Redis 기반)
 * 
 * Redis에 인증번호를 저장하고 TTL로 자동 만료 처리합니다.
 * 세션 토큰 방식으로 보안을 강화합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserAuthRepository userAuthRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${password.reset.token-validity:10}")
    private int codeValidityMinutes;

    private static final String REDIS_KEY_PREFIX = "password:reset:code:";
    private static final String REDIS_KEY_VERIFIED_PREFIX = "password:reset:verified:";
    private static final String REDIS_KEY_TOKEN_PREFIX = "password:reset:token:";

    @Override
    public void requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        Optional<UserAuth> userAuthOpt = userAuthRepository.findByEmailNormalized(normalizedEmail);
        if (userAuthOpt.isEmpty()) {
            log.warn("비밀번호 재설정 요청: 존재하지 않는 이메일 - {}", normalizedEmail);
            return;
        }

        String code = generateVerificationCode();
        String redisKey = REDIS_KEY_PREFIX + normalizedEmail;
        redisTemplate.delete(redisKey);

        String hashedCode = hashCode(code);
        redisTemplate.opsForValue().set(redisKey, hashedCode, codeValidityMinutes, TimeUnit.MINUTES);

        try {
            emailService.sendPasswordResetCode(normalizedEmail, code);
            log.info("비밀번호 재설정 인증번호 생성 및 이메일 전송 완료: {}", normalizedEmail);
        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일 전송 실패: {}", normalizedEmail, e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    @Override
    public String verifyCode(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase();

        String redisKey = REDIS_KEY_PREFIX + normalizedEmail;
        String storedHashedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedHashedCode == null) {
            throw new IllegalArgumentException("인증번호가 만료되었거나 존재하지 않습니다.");
        }

        String hashedCode = hashCode(code);
        if (!MessageDigest.isEqual(hashedCode.getBytes(), storedHashedCode.getBytes())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // 세션 토큰 생성 및 저장
        String sessionToken = generateSessionToken();
        String tokenKey = REDIS_KEY_TOKEN_PREFIX + sessionToken;
        redisTemplate.opsForValue().set(tokenKey, normalizedEmail, 10, TimeUnit.MINUTES);
        
        String verifiedKey = REDIS_KEY_VERIFIED_PREFIX + normalizedEmail;
        redisTemplate.opsForValue().set(verifiedKey, "verified", 10, TimeUnit.MINUTES);

        redisTemplate.delete(redisKey);

        log.info("비밀번호 재설정 인증번호 검증 완료: {}", normalizedEmail);
        
        return sessionToken;
    }

    @Override
    @Transactional
    public void resetPassword(String sessionToken, String newPassword) {
        String tokenKey = REDIS_KEY_TOKEN_PREFIX + sessionToken;
        String normalizedEmail = redisTemplate.opsForValue().get(tokenKey);
        
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("세션이 만료되었거나 유효하지 않은 토큰입니다.");
        }
        
        String verifiedKey = REDIS_KEY_VERIFIED_PREFIX + normalizedEmail;
        String verified = redisTemplate.opsForValue().get(verifiedKey);
        
        if (verified == null || !verified.equals("verified")) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        Optional<UserAuth> userAuthOpt = userAuthRepository.findByEmailNormalized(normalizedEmail);
        if (userAuthOpt.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        UserAuth userAuth = userAuthOpt.get();
        String encodedPassword = passwordEncoder.encode(newPassword);
        userAuth.updatePasswordHash(encodedPassword);
        userAuthRepository.save(userAuth);

        redisTemplate.delete(tokenKey);
        redisTemplate.delete(verifiedKey);

        log.info("비밀번호 재설정 완료: {}", normalizedEmail);
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
    
    private String generateSessionToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        
        for (int i = 0; i < 32; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return token.toString();
    }

    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시 생성 실패", e);
        }
    }
}

