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

    /**
     * 비밀번호 재설정 요청: 6자리 인증번호를 생성하고 이메일로 전송합니다.
     *
     * @param email 재설정할 사용자의 이메일 주소
     */
    @Override
    public void requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        // 사용자 존재 여부 확인 (보안을 위해 존재하지 않아도 성공 메시지 반환)
        Optional<UserAuth> userAuthOpt = userAuthRepository.findByEmailNormalized(normalizedEmail);
        if (userAuthOpt.isEmpty()) {
            log.warn("비밀번호 재설정 요청: 존재하지 않는 이메일 - {}", normalizedEmail);
            return;
        }

        // 6자리 인증번호 생성
        String code = generateVerificationCode();
        String redisKey = REDIS_KEY_PREFIX + normalizedEmail;
        
        // 기존 요청이 있으면 삭제 (중복 방지)
        redisTemplate.delete(redisKey);

        // Redis에 해시된 값 저장 (TTL 적용)
        String hashedCode = hashCode(code);
        redisTemplate.opsForValue().set(redisKey, hashedCode, codeValidityMinutes, TimeUnit.MINUTES);

        // 이메일 전송
        try {
            emailService.sendPasswordResetCode(normalizedEmail, code);
            log.info("비밀번호 재설정 인증번호 생성 및 이메일 전송 완료: {}", normalizedEmail);
        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일 전송 실패: {}", normalizedEmail, e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    /**
     * 인증번호 검증: 이메일 인증 단계를 완료하고 세션 토큰을 발급합니다.
     *
     * @param email 이메일 주소
     * @param code 6자리 인증번호
     * @return 세션 토큰 (비밀번호 재설정 시 사용)
     * @throws IllegalArgumentException 인증번호가 만료되었거나 일치하지 않을 때
     */
    @Override
    public String verifyCode(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase();

        // Redis에서 저장된 인증번호 조회
        String redisKey = REDIS_KEY_PREFIX + normalizedEmail;
        String storedHashedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedHashedCode == null) {
            throw new IllegalArgumentException("인증번호가 만료되었거나 존재하지 않습니다.");
        }

        // 인증번호 해시 비교 (타이밍 공격 방지)
        String hashedCode = hashCode(code);
        if (!MessageDigest.isEqual(hashedCode.getBytes(), storedHashedCode.getBytes())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // 세션 토큰 생성 및 Redis에 저장 (10분 TTL)
        String sessionToken = generateSessionToken();
        String tokenKey = REDIS_KEY_TOKEN_PREFIX + sessionToken;
        redisTemplate.opsForValue().set(tokenKey, normalizedEmail, 10, TimeUnit.MINUTES);
        
        // 인증 완료 상태 저장 (추가 보안 레이어)
        String verifiedKey = REDIS_KEY_VERIFIED_PREFIX + normalizedEmail;
        redisTemplate.opsForValue().set(verifiedKey, "verified", 10, TimeUnit.MINUTES);

        // 원본 인증번호는 삭제 (일회용)
        redisTemplate.delete(redisKey);

        log.info("비밀번호 재설정 인증번호 검증 완료: {}", normalizedEmail);
        
        return sessionToken;
    }

    /**
     * 비밀번호 재설정: 세션 토큰으로 권한을 확인하고 새로운 비밀번호로 업데이트합니다.
     *
     * @param sessionToken 세션 토큰
     * @param newPassword 새로운 비밀번호
     * @throws IllegalArgumentException 세션 토큰이 만료되었거나 인증이 완료되지 않았을 때
     */
    @Override
    @Transactional
    public void resetPassword(String sessionToken, String newPassword) {
        // 세션 토큰으로 이메일 확인
        String tokenKey = REDIS_KEY_TOKEN_PREFIX + sessionToken;
        String normalizedEmail = redisTemplate.opsForValue().get(tokenKey);
        
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("세션이 만료되었거나 유효하지 않은 토큰입니다.");
        }
        
        // 추가 보안 레이어: 인증 완료 상태 확인
        String verifiedKey = REDIS_KEY_VERIFIED_PREFIX + normalizedEmail;
        String verified = redisTemplate.opsForValue().get(verifiedKey);
        
        if (verified == null || !verified.equals("verified")) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        // 사용자 조회
        Optional<UserAuth> userAuthOpt = userAuthRepository.findByEmailNormalized(normalizedEmail);
        if (userAuthOpt.isEmpty()) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 비밀번호 해시 처리 및 업데이트
        UserAuth userAuth = userAuthOpt.get();
        String encodedPassword = passwordEncoder.encode(newPassword);
        userAuth.updatePasswordHash(encodedPassword);
        userAuthRepository.save(userAuth);

        // 세션 토큰 및 인증 상태 삭제 (일회용)
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

