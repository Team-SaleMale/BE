package com.salemale.domain.auth.service;

import com.salemale.domain.user.entity.UserAuth;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.auth.dto.request.SignupRequest;
import com.salemale.domain.user.repository.UserRepository;
import com.salemale.domain.user.repository.UserAuthRepository;
import com.salemale.global.common.enums.LoginType;
import com.salemale.global.security.jwt.JwtTokenProvider;
import com.salemale.common.exception.GeneralException;
import com.salemale.common.code.status.ErrorStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthServiceImpl: 인증 관련 비즈니스 로직 구현체
 *
 * 로그인/회원가입/중복 검사 등의 실제 로직을 처리합니다.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserAuthRepository userAuthRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(
            UserAuthRepository userAuthRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userAuthRepository = userAuthRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 로컬 로그인: 사용자가 입력한 자격 증명을 검증하고 JWT 토큰을 발급합니다.
     *
     * @param email 사용자가 입력한 이메일(로그인 ID)
     * @param rawPassword 사용자가 입력한 평문 비밀번호
     * @return JWT 액세스 토큰 문자열
     * @throws GeneralException 이메일이 존재하지 않거나 비밀번호가 일치하지 않을 때 발생
     */
    @Override
    public String loginLocal(String email, String rawPassword) {
        String normalized = email.trim().toLowerCase();

        UserAuth auth = userAuthRepository.findByProviderAndEmailNormalized(LoginType.LOCAL, normalized)
                .orElseThrow(() -> new GeneralException(ErrorStatus.AUTH_INVALID_CREDENTIALS));

        String hash = auth.getPasswordHash();
        if (hash == null || !passwordEncoder.matches(rawPassword, hash)) {
            throw new GeneralException(ErrorStatus.AUTH_INVALID_CREDENTIALS);
        }

        User user = auth.getUser();
        String subjectUserId = String.valueOf(user.getId());
        return jwtTokenProvider.generateToken(subjectUserId);
    }

    /**
     * 로컬 회원가입: 새로운 사용자를 생성하고 이메일/비밀번호 인증 정보를 등록합니다.
     *
     * @param request 회원가입 요청 정보(이메일, 닉네임, 비밀번호)
     */
    @Override
    @Transactional
    public void registerLocal(SignupRequest request) {
        String normalized = request.getEmail().trim().toLowerCase();

        userAuthRepository.findByProviderAndEmailNormalized(LoginType.LOCAL, normalized)
                .ifPresent(a -> { throw new GeneralException(ErrorStatus.USER_EMAIL_ALREADY_EXISTS); });

        User user = User.builder()
                .nickname(request.getNickname())
                .email(request.getEmail())
                .build();
        user = userRepository.save(user);

        String hash = passwordEncoder.encode(request.getPassword());

        UserAuth auth = UserAuth.builder()
                .user(user)
                .provider(LoginType.LOCAL)
                .emailNormalized(normalized)
                .passwordHash(hash)
                .build();
        userAuthRepository.save(auth);
    }

    /**
     * 이메일 중복 확인: 회원가입 전 해당 이메일이 이미 사용 중인지 검사합니다.
     *
     * @param email 검사할 이메일 주소
     * @return true이면 이미 사용 중, false이면 사용 가능
     */
    @Override
    public boolean existsLocalEmail(String email) {
        String normalized = email.trim().toLowerCase();
        return userAuthRepository.existsByProviderAndEmailNormalized(LoginType.LOCAL, normalized);
    }

    /**
     * 닉네임 중복 확인: 회원가입 전 해당 닉네임이 이미 사용 중인지 검사합니다.
     *
     * @param nickname 검사할 닉네임
     * @return true이면 이미 사용 중, false이면 사용 가능
     */
    @Override
    public boolean existsNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}

