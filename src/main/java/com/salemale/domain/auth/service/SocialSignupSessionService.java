package com.salemale.domain.auth.service;

import com.salemale.global.common.enums.LoginType;

/**
 * 소셜 로그인 성공 후 최종 회원가입 확정을 위한 세션 토큰 관리 서비스.
 */
public interface SocialSignupSessionService {

    /**
     * 소셜 세션 저장 후 토큰 반환.
     */
    String create(LoginType provider, String providerUserId, String email);

    /**
     * 토큰 유효성 검증 및 정보 조회.
     */
    SocialSession get(String token);

    /**
     * 사용 완료 시 세션 제거.
     */
    void consume(String token);

    record SocialSession(LoginType provider, String providerUserId, String email) {}
}


