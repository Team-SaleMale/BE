package com.salemale.domain.user.entity; // 도메인: 사용자 인증수단(로컬/소셜) 관리 테이블

import com.salemale.global.common.BaseEntity; // 공통 엔티티(생성/수정 시간 등)
import com.salemale.global.common.enums.LoginType; // 인증 제공자 타입(LOCAL/KAKAO/NAVER)
import jakarta.persistence.*; // JPA 매핑 애노테이션 전반
import lombok.*; // 롬복(보일러플레이트 제거)

@Entity // JPA 엔티티 선언
@Table(
        name = "user_auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_auth_provider_user", columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uk_user_auth_provider_email", columnNames = {"provider", "email_normalized"})
        },
        indexes = {
                @Index(name = "idx_user_auth_provider_email", columnList = "provider,email_normalized"),
                @Index(name = "idx_user_auth_email_normalized", columnList = "email_normalized")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserAuth extends BaseEntity { // 인증 레코드(1 user : N auth)

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // IDENTITY 전략 사용
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // 다대일: 여러 인증수단이 한 사용자에 연결
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_auth_user")) // FK 명시로 스키마 가독성 향상
    private User user;

    @Enumerated(EnumType.STRING) // 문자열 저장(순서 변경 안전)
    @Column(name = "provider", nullable = false, columnDefinition = "VARCHAR(20)")
    private LoginType provider;

    // LOCAL은 null, 소셜은 공급자 유저 ID 저장(예: 카카오 id)
    @Column(name = "provider_user_id", length = 100)
    private String providerUserId;

    // 이메일 로그인용(소문자 정규화 저장), 소셜은 미제공 가능(이 경우 provider_user_id로만 식별)
    @Column(name = "email_normalized", length = 254)
    private String emailNormalized;

    // LOCAL만 비밀번호 해시 저장(BCrypt 등): 원문 비밀번호는 저장 금지
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "last_login_at") // 최근 로그인 시간 기록(보안/통계용)
    private java.time.LocalDateTime lastLoginAt;

    /**
     * 비밀번호 해시를 업데이트합니다.
     *
     * - 비밀번호 변경 시 새로운 해시값으로 교체합니다.
     * - **평문 비밀번호가 아닌 BCrypt로 해시된 값**을 전달해야 합니다.
     * - BCrypt 해시 형식 검증: $2a$, $2b$, $2y$ 버전과 올바른 형식만 허용합니다.
     * - LOCAL 인증 제공자에서만 사용됩니다.
     *
     * **호출자 책임:**
     * - 이 메서드는 이미 BCrypt로 해시된 값을 받습니다.
     * - 평문 비밀번호를 전달하면 안 됩니다.
     * - 서비스 계층에서 PasswordEncoder.encode()를 먼저 호출해야 합니다.
     *
     * BCrypt 해시 형식 예시:
     * - $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
     * - $2b$12$ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuv (60자)
     *
     * @param newPasswordHash 새로운 비밀번호 해시 (BCrypt 형식)
     * @throws IllegalArgumentException BCrypt 해시 형식이 올바르지 않을 때
     */
    public void updatePasswordHash(String newPasswordHash) {
        // 1) null 또는 빈 문자열은 무시: 소셜 로그인 계정은 비밀번호가 없을 수 있습니다.
        if (newPasswordHash == null || newPasswordHash.isEmpty()) {
            return;
        }
        
        // 2) BCrypt 해시 형식 검증: $2[a|b|y]$[cost]$[22자 salt][31자 hash]
        //    - 형식: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
        //    - 총 길이: 60자 (버전 3자 + cost 3자 + $ 1자 + salt 22자 + hash 31자)
        //    - 정규식: ^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$
        if (!newPasswordHash.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$")) {
            // 2-1) 형식이 올바르지 않으면 예외를 발생시킵니다.
            //      - 평문 비밀번호가 전달된 경우 여기서 걸러집니다.
            //      - 에러 메시지에는 처음 10자만 포함하여 전체 해시가 노출되지 않도록 합니다.
            throw new IllegalArgumentException(
                "Invalid BCrypt hash format. Expected BCrypt hash (e.g., $2a$10$...), but got: " 
                + newPasswordHash.substring(0, Math.min(10, newPasswordHash.length())) + "..."
            );
        }
        
        // 3) BCrypt 해시는 공백이 포함되면 안 되므로 trim하지 않고 그대로 저장합니다.
        //    - BCrypt 해시는 정확히 60자이고, 공백이 있으면 위 정규식에서 이미 거부됩니다.
        this.passwordHash = newPasswordHash;
    }
}


