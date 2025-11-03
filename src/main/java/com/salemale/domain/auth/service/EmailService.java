package com.salemale.domain.auth.service;

/**
 * EmailService: 이메일 전송 서비스 인터페이스
 */
public interface EmailService {

    /**
     * 비밀번호 재설정용 인증번호를 이메일로 전송합니다.
     *
     * @param to 수신자 이메일 주소
     * @param code 6자리 인증번호
     */
    void sendPasswordResetCode(String to, String code);

    /**
     * 회원가입 이메일 인증용 인증번호를 전송합니다.
     * @param to 수신자 이메일
     * @param code 6자리 인증번호
     */
    void sendSignupVerificationCode(String to, String code);
}

