package com.salemale.domain.user.repository; // UserAuth 엔티티용 리포지토리(인증수단 조회 용도)

import com.salemale.domain.user.entity.User; // 사용자 엔티티
import com.salemale.domain.user.entity.UserAuth; // 인증수단 엔티티
import com.salemale.global.common.enums.LoginType; // 제공자 타입
import org.springframework.data.jpa.repository.JpaRepository; // JPA 표준 리포지토리

import java.util.Optional; // Optional 반환으로 존재 여부 명확화

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
    Optional<UserAuth> findByProviderAndEmailNormalized(LoginType provider, String emailNormalized);
    Optional<UserAuth> findByProviderAndProviderUserId(LoginType provider, String providerUserId);
    boolean existsByProviderAndEmailNormalized(LoginType provider, String emailNormalized);
    
    // 특정 사용자의 특정 제공자 인증 정보 조회 (비밀번호 변경 등에 사용)
    Optional<UserAuth> findByProviderAndUser(LoginType provider, User user);
    
    // 이메일로 인증 정보 조회 (비밀번호 재설정 등에 사용)
    Optional<UserAuth> findByEmailNormalized(String emailNormalized);
}


