package com.salemale.domain.user.service; // 인증(로그인) 유스케이스를 담당하는 서비스 레이어

import com.salemale.domain.user.entity.UserAuth; // 인증수단 엔티티(LOCAL/소셜)
import com.salemale.domain.user.entity.User; // 사용자 프로필 엔티티
import com.salemale.domain.user.dto.request.SignupRequest; // 회원가입 요청 DTO
import com.salemale.domain.user.repository.UserRepository; // User 저장소
import com.salemale.domain.user.repository.UserAuthRepository; // 인증수단 조회용 리포지토리
import com.salemale.global.common.enums.LoginType; // 인증 제공자 타입(LOCAL, KAKAO, ...)
import com.salemale.global.security.jwt.JwtTokenProvider; // 액세스 토큰 생성기
import com.salemale.common.exception.GeneralException; // 커스텀 예외
import com.salemale.common.code.status.ErrorStatus; // 에러 코드 집합
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 해시 검증용
import org.springframework.stereotype.Service; // 서비스 빈 선언
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 처리

@Service
public class AuthService { // 응용 서비스: 컨트롤러와 리포지토리 사이 비즈니스 로직 구현

    private final UserAuthRepository userAuthRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserAuthRepository userAuthRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userAuthRepository = userAuthRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String loginLocal(String email, String rawPassword) {
        String normalized = email.trim().toLowerCase(); // 이메일 정규화(대소문자 이슈 제거)
        UserAuth auth = userAuthRepository.findByProviderAndEmailNormalized(LoginType.LOCAL, normalized)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        // 존재하지 않으면 인증 실패 처리

        String hash = auth.getPasswordHash();
        if (hash == null || !passwordEncoder.matches(rawPassword, hash)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        // 비밀번호 불일치 시 실패

        return jwtTokenProvider.generateToken(normalized); // 정상 시 JWT 발급(추후 userId/roles 등 클레임 확장 가능)
    }

    @Transactional
    public void registerLocal(SignupRequest request) {
        String normalized = request.getEmail().trim().toLowerCase();
        // 중복 이메일 확인(LOCAL 자격 기준)
        userAuthRepository.findByProviderAndEmailNormalized(LoginType.LOCAL, normalized)
                .ifPresent(a -> { throw new GeneralException(ErrorStatus.USER_EMAIL_ALREADY_EXISTS); });

        // 프로필 생성(표시용 이메일은 선택)
        User user = User.builder()
                .nickname(request.getNickname())
                .email(request.getEmail())
                .build();
        user = userRepository.save(user);

        // 비밀번호 해시 후 로컬 인증수단 저장
        String hash = passwordEncoder.encode(request.getPassword());
        UserAuth auth = UserAuth.builder()
                .user(user)
                .provider(LoginType.LOCAL)
                .emailNormalized(normalized)
                .passwordHash(hash)
                .build();
        userAuthRepository.save(auth);
    }
}


