package com.salemale.domain.user.entity; // 도메인: 사용자 프로필(인증수단은 UserAuth로 분리)

import com.salemale.global.common.BaseEntity; // 생성/수정 시간 등을 제공하는 공통 엔티티 상속
import com.salemale.global.common.enums.AlarmChecked; // 알림 허용 여부를 표현하는 ENUM
import com.salemale.global.common.enums.LoginType; // (참고) 로그인 제공자 ENUM. UserAuth에서 사용. User는 프로필만 보관
import jakarta.persistence.*; // JPA 매핑 애노테이션 패키지 전반 사용(@Entity, @Column 등)
import lombok.AccessLevel; // 생성자 접근 제한 수준 지정(PROTECTED)
import lombok.AllArgsConstructor; // 모든 필드를 받는 생성자 자동 생성
import lombok.Builder; // 빌더 패턴 자동 생성
import lombok.Getter; // 필드에 대한 getter 자동 생성
import lombok.NoArgsConstructor; // 파라미터 없는 생성자 자동 생성

@Entity // JPA 엔티티로 매핑됨(테이블 레코드와 1:1 대응)
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_phone_number", columnList = "phone_number")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseEntity {

    @Id // 기본 키
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB의 AUTO_INCREMENT/IDENTITY 전략 사용
    private Long id;

    @Column(name = "nickname", nullable = false, length = 15) // UI에 표시되는 별칭(서비스 정책에 따라 유니크 고려 가능)
    private String nickname;

    // 소셜 로그인에서 이메일 미제공이 가능하므로 nullable 허용
    // 로컬 로그인 자격은 UserAuth.emailNormalized에 저장되고, User.email은 프로필 표시/연락 용도로 사용 가능
    @Column(name = "email", nullable = true, length = 254)
    private String email;

    // 경매지수(혹은 매너지수): 사용자 신뢰도/매너 지표로 사용
    @Builder.Default // 빌더 사용 시 기본값 50 유지
    @Column(name = "manner_score", nullable = false)
    private Integer mannerScore = 50;

    // 반영할 거리 설정(1: 가까움, 2: 조금 먼, 3: 먼)
    @Enumerated(EnumType.STRING) // 문자열로 저장하여 enum 순서 변경의 위험 회피
    @Column(name = "range_setting")
    private RangeSetting rangeSetting;

    @Column(name = "profile_image", length = 200) // 프로필 이미지 URL 등 경로 저장
    private String profileImage;

    @Builder.Default // 기본값 NO
    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_checked", nullable = false, columnDefinition = "VARCHAR(20)")
    private AlarmChecked alarmChecked = AlarmChecked.NO;

    // 휴대폰 본인인증 확장 대비: E.164(+821012345678) 규격 권장, 유니크로 중복 방지
    @Column(name = "phone_number", length = 20, unique = true)
    private String phoneNumber;

    @Column(name = "phone_verified_at") // 본인인증(휴대폰) 완료 시각 기록(존재 유무로 인증 여부 판단 가능)
    private java.time.LocalDateTime phoneVerifiedAt;

    public enum RangeSetting {
        NEAR, MEDIUM, FAR // 가까움/중간/먼 거리 설정을 영문 상수로 관리(다국어 표시는 프론트/리소스에서 처리)
    }
}
