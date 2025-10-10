package com.salemale.domain.user.service; // 인증(로그인) 유스케이스를 담당하는 서비스 구현체

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

/**
 * AuthServiceImpl: 인증 관련 비즈니스 로직을 실제로 구현하는 서비스 클래스입니다.
 *
 * - AuthService 인터페이스를 구현합니다.
 * - 로그인/회원가입/중복 검사 등의 실제 로직을 처리합니다.
 * - 스프링 빈으로 등록되어 컨트롤러나 다른 서비스에서 주입받아 사용할 수 있습니다.
 *
 * 주요 책임:
 * 1. 사용자 인증 정보 검증 및 JWT 토큰 발급
 * 2. 새로운 사용자 등록 및 비밀번호 해시 처리
 * 3. 이메일/닉네임 중복 여부 확인
 */
@Service // 스프링이 이 클래스를 서비스 빈으로 등록하여 다른 곳에서 주입받을 수 있게 합니다.
public class AuthServiceImpl implements AuthService { // AuthService 인터페이스 구현

    // 의존성 선언: 각 필드는 생성자를 통해 스프링이 자동으로 주입해 줍니다.
    private final UserAuthRepository userAuthRepository; // 사용자 인증 정보를 조회/저장하는 저장소
    private final UserRepository userRepository; // 사용자 프로필을 조회/저장하는 저장소
    private final PasswordEncoder passwordEncoder; // 비밀번호를 안전하게 해시하고 검증하는 도구
    private final JwtTokenProvider jwtTokenProvider; // JWT 액세스 토큰을 생성하는 도구

    // 생성자 주입: 스프링이 애플리케이션 시작 시 필요한 빈들을 자동으로 넣어줍니다.
    public AuthServiceImpl(
            UserAuthRepository userAuthRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userAuthRepository = userAuthRepository; // 인증 정보 저장소 주입
        this.userRepository = userRepository; // 사용자 저장소 주입
        this.passwordEncoder = passwordEncoder; // 비밀번호 인코더 주입
        this.jwtTokenProvider = jwtTokenProvider; // JWT 토큰 생성기 주입
    }

    /**
     * 로컬 로그인(이메일/비밀번호): 사용자가 입력한 자격 증명을 검증하고 JWT 토큰을 발급합니다.
     *
     * @param email 사용자가 입력한 이메일(로그인 ID)
     * @param rawPassword 사용자가 입력한 평문 비밀번호
     * @return JWT 액세스 토큰 문자열
     * @throws GeneralException 이메일이 존재하지 않거나 비밀번호가 일치하지 않을 때 발생
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    public String loginLocal(String email, String rawPassword) {
        // 1) 이메일 정규화: 공백을 제거하고 소문자로 통일하여 대소문자 차이로 인한 로그인 실패를 방지합니다.
        String normalized = email.trim().toLowerCase();

        // 2) 데이터베이스에서 LOCAL 타입의 인증 정보를 찾습니다.
        //    - findByProviderAndEmailNormalized: 특정 제공자(LOCAL)와 정규화된 이메일로 조회
        //    - orElseThrow: 찾지 못하면 예외를 던져 인증 실패로 처리
        UserAuth auth = userAuthRepository.findByProviderAndEmailNormalized(LoginType.LOCAL, normalized)
                .orElseThrow(() -> new GeneralException(ErrorStatus.AUTH_INVALID_CREDENTIALS));

        // 3) 저장된 비밀번호 해시를 가져옵니다.
        String hash = auth.getPasswordHash();

        // 4) 비밀번호 검증: 해시가 없거나 사용자가 입력한 평문과 일치하지 않으면 인증 실패
        //    - passwordEncoder.matches: BCrypt 등으로 해시된 비밀번호와 평문을 비교
        if (hash == null || !passwordEncoder.matches(rawPassword, hash)) {
            throw new GeneralException(ErrorStatus.AUTH_INVALID_CREDENTIALS);
        }

        // 5) 모든 검증을 통과했으므로 JWT 토큰을 생성하여 반환합니다.
        //    - subject를 userId로 발급하여 이메일 변경과 무관하게 식별하도록 합니다.
        return jwtTokenProvider.generateToken(String.valueOf(auth.getUser().getId()));
    }

    /**
     * 로컬 회원가입: 새로운 사용자를 생성하고 이메일/비밀번호 인증 정보를 등록합니다.
     *
     * @param request 회원가입 요청 정보(이메일, 닉네임, 비밀번호)
     * @throws GeneralException 이미 등록된 이메일인 경우 발생
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    @Transactional // 사용자 생성과 인증 정보 저장을 하나의 트랜잭션으로 묶어 일관성을 보장합니다.
    public void registerLocal(SignupRequest request) {
        // 1) 이메일 정규화: 공백 제거 및 소문자 변환으로 중복 검사 정확도를 높입니다.
        String normalized = request.getEmail().trim().toLowerCase();

        // 2) 중복 이메일 확인: LOCAL 타입으로 이미 등록된 이메일이 있는지 검사합니다.
        //    - ifPresent: Optional에 값이 있으면(= 중복이면) 예외를 던집니다.
        userAuthRepository.findByProviderAndEmailNormalized(LoginType.LOCAL, normalized)
                .ifPresent(a -> { throw new GeneralException(ErrorStatus.USER_EMAIL_ALREADY_EXISTS); });

        // 3) 사용자 프로필 생성: User 엔티티를 만들고 닉네임과 표시용 이메일을 저장합니다.
        //    - Builder 패턴을 사용하여 가독성 높게 객체를 생성합니다.
        User user = User.builder()
                .nickname(request.getNickname()) // 사용자가 입력한 닉네임
                .email(request.getEmail()) // 표시용 이메일(원본 형태 유지)
                .build();
        user = userRepository.save(user); // 데이터베이스에 저장 후 ID가 부여된 객체를 받습니다.

        // 4) 비밀번호 해시 생성: 평문 비밀번호를 BCrypt 등으로 안전하게 암호화합니다.
        //    - 절대 평문 비밀번호를 저장하지 마세요! 보안의 기본 원칙입니다.
        String hash = passwordEncoder.encode(request.getPassword());

        // 5) 인증 정보 저장: UserAuth 엔티티를 생성하여 로그인 수단을 등록합니다.
        //    - provider: LOCAL(이메일/비밀번호 방식)
        //    - emailNormalized: 정규화된 이메일로 로그인 시 검색에 사용
        //    - passwordHash: 해시된 비밀번호
        UserAuth auth = UserAuth.builder()
                .user(user) // 방금 생성한 사용자와 연결
                .provider(LoginType.LOCAL) // 로컬 인증 방식
                .emailNormalized(normalized) // 검색용 정규화 이메일
                .passwordHash(hash) // 해시된 비밀번호
                .build();
        userAuthRepository.save(auth); // 인증 정보를 데이터베이스에 저장
    }

    /**
     * 이메일 중복 확인: 회원가입 전 해당 이메일이 이미 사용 중인지 검사합니다.
     *
     * @param email 검사할 이메일 주소
     * @return true이면 이미 사용 중, false이면 사용 가능
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    public boolean existsLocalEmail(String email) {
        // 1) 이메일 정규화: 입력값을 trim하고 소문자로 변환하여 일관되게 검사합니다.
        String normalized = email.trim().toLowerCase();

        // 2) LOCAL 제공자 기준으로 해당 이메일이 등록되어 있는지 확인합니다.
        //    - existsByProviderAndEmailNormalized: 데이터베이스에 레코드가 있으면 true 반환
        //    - 주의: 계정 열거(account enumeration) 공격을 방지하기 위해 프론트엔드에서 레이트리밋을 적용하세요.
        return userAuthRepository.existsByProviderAndEmailNormalized(LoginType.LOCAL, normalized);
    }

    /**
     * 닉네임 중복 확인: 회원가입 전 해당 닉네임이 이미 사용 중인지 검사합니다.
     *
     * @param nickname 검사할 닉네임
     * @return true이면 이미 사용 중, false이면 사용 가능
     */
    @Override // 인터페이스 메서드 구현을 명시적으로 표시
    public boolean existsNickname(String nickname) {
        // 프로필(User) 테이블에서 닉네임 중복 여부를 검사합니다.
        // - existsByNickname: 해당 닉네임이 이미 존재하면 true 반환
        // - 닉네임은 사용자에게 표시되는 이름이므로 중복을 허용하지 않습니다.
        return userRepository.existsByNickname(nickname);
    }
}

