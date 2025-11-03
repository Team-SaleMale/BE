package com.salemale.domain.auth.service;

/**
 * 회원가입 이메일 인증 서비스.
 * <p>
 * - 회원가입 이전 단계에서 6자리 인증코드(숫자)를 이메일로 발송/검증합니다.
 * - 코드 검증에 성공하면 단기 세션 토큰을 발급하여 회원가입 API 호출 시 헤더로 제출하게 합니다.
 * - 토큰과 코드는 TTL(만료 시간)을 가지고 자동 소멸됩니다.
 */
public interface SignupVerificationService {

    /**
     * 회원가입용 6자리 인증코드를 발송합니다(이미 가입된 이메일은 상위 레이어에서 차단).
     * @param email 대상 이메일(케이스 인센서티브 처리 권장)
     */
    void sendSignupCode(String email);

    /**
     * 전달된 인증코드를 검증하고, 유효 시 단기 세션 토큰을 발급합니다.
     * @param email 이메일
     * @param code 6자리 인증코드
     * @return 회원가입 완료 시까지 사용할 단기 세션 토큰
     * @throws IllegalArgumentException 코드가 없거나 만료/불일치 시
     */
    String verifySignupCode(String email, String code);

    /**
     * 회원가입 요청 시 제출된 단기 세션 토큰의 유효성을 확인합니다.
     * @param email 회원가입 대상 이메일(토큰 소유자와 일치해야 함)
     * @param token 단기 세션 토큰
     * @return 유효하면 true
     */
    boolean validateSignupToken(String email, String token);
}


