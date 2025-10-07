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
                @Index(name = "idx_user_auth_provider_email", columnList = "provider,email_normalized")
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
}


