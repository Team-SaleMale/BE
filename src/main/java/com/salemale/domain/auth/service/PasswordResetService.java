package com.salemale.domain.auth.service;

/**
 * PasswordResetService: 비밀번호 재설정 서비스 인터페이스 (Redis 기반)
 * 
 * DB 테이블 추가 없이 Redis에 인증번호를 저장합니다.
 * TTL(Time To Live)로 자동 만료 처리됩니다.
 */
public interface PasswordResetService {

    /**
     * 비밀번호 재설정 인증번호를 생성하고 이메일로 전송합니다.
     *
     * @param email 재설정할 사용자의 이메일 주소
     */
    void requestPasswordReset(String email);

    /**
     * 인증번호를 검증합니다. (이메일 인증 단계)
     *
     * @param email 이메일 주소
     * @param code 6자리 인증번호
     * @return 세션 토큰
     */
    String verifyCode(String email, String code);

    /**
     * 비밀번호를 재설정합니다. (인증 완료 후)
     *
     * @param sessionToken 세션 토큰
     * @param newPassword 새로운 비밀번호
     */
    void resetPassword(String sessionToken, String newPassword);
}

