package com.salemale.domain.user.service; // 인증(로그인/회원가입) 서비스 인터페이스

import com.salemale.domain.user.dto.request.SignupRequest; // 회원가입 요청 DTO

/**
 * AuthService: 인증 관련 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 *
 * - 로컬(이메일/비밀번호) 로그인 및 회원가입 기능을 제공합니다.
 * - 이메일/닉네임 중복 체크 기능을 제공합니다.
 * - 구현체(AuthServiceImpl)에서 실제 비즈니스 로직을 처리합니다.
 *
 * 주요 기능:
 * 1. 로그인: 이메일/비밀번호를 검증하고 JWT 토큰을 발급합니다.
 * 2. 회원가입: 새로운 사용자를 생성하고 인증 정보를 저장합니다.
 * 3. 중복 검사: 이메일/닉네임이 이미 사용 중인지 확인합니다.
 */
public interface AuthService {

    /**
     * 로컬 로그인(이메일/비밀번호): 사용자가 입력한 자격 증명을 검증하고 JWT 토큰을 발급합니다.
     *
     * @param email 사용자가 입력한 이메일(로그인 ID)
     * @param rawPassword 사용자가 입력한 평문 비밀번호
     * @return JWT 액세스 토큰 문자열
     * @throws com.salemale.common.exception.GeneralException 이메일이 존재하지 않거나 비밀번호가 일치하지 않을 때 발생
     */
    String loginLocal(String email, String rawPassword);

    /**
     * 로컬 회원가입: 새로운 사용자를 생성하고 이메일/비밀번호 인증 정보를 등록합니다.
     *
     * @param request 회원가입 요청 정보(이메일, 닉네임, 비밀번호)
     * @throws com.salemale.common.exception.GeneralException 이미 등록된 이메일인 경우 발생
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
