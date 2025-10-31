package com.salemale.domain.auth.service;

import com.salemale.domain.auth.dto.request.SignupRequest;

/**
 * AuthService: 인증 관련 비즈니스 로직을 정의하는 서비스 인터페이스
 *
 * 로컬(이메일/비밀번호) 로그인 및 회원가입 기능을 제공합니다.
 * 이메일/닉네임 중복 체크 기능을 제공합니다.
 */
public interface AuthService {

    /**
     * 로컬 로그인: 사용자가 입력한 자격 증명을 검증하고 JWT 토큰을 발급합니다.
     *
     * @param email 사용자가 입력한 이메일(로그인 ID)
     * @param rawPassword 사용자가 입력한 평문 비밀번호
     * @return JWT 액세스 토큰 문자열
     */
    String loginLocal(String email, String rawPassword);

    /**
     * 로컬 회원가입: 새로운 사용자를 생성하고 이메일/비밀번호 인증 정보를 등록합니다.
     *
     * @param request 회원가입 요청 정보(이메일, 닉네임, 비밀번호)
     */
    void registerLocal(SignupRequest request);

    /**
     * 이메일 중복 확인: 회원가입 전 해당 이메일이 이미 사용 중인지 검사합니다.
     *
     * @param email 검사할 이메일 주소
     * @return true이면 이미 사용 중, false이면 사용 가능
     */
    boolean existsLocalEmail(String email);

    /**
     * 닉네임 중복 확인: 회원가입 전 해당 닉네임이 이미 사용 중인지 검사합니다.
     *
     * @param nickname 검사할 닉네임
     * @return true이면 이미 사용 중, false이면 사용 가능
     */
    boolean existsNickname(String nickname);
}

