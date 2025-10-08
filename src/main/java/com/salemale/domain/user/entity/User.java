package com.salemale.domain.user.entity; // 도메인: 사용자 프로필(인증수단은 UserAuth로 분리)

import com.salemale.global.common.BaseEntity; // 생성/수정 시간 등을 제공하는 공통 엔티티 상속
import com.salemale.global.common.enums.AlarmChecked; // 알림 허용 여부를 표현하는 ENUM
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

    // 반영할 거리 설정
    @Builder.Default
    @Enumerated(EnumType.STRING) // 문자열로 저장하여 enum 순서 변경의 위험 회피
    @Column(name = "range_setting")
    private RangeSetting rangeSetting=RangeSetting.NEAR;

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

    /**
     * RangeSetting: 사용자의 활동 반경을 설정하는 열거형입니다.
     *
     * - 각 설정은 특정 거리(km)에 매핑됩니다.
     * - 지역 기반 검색/필터링 시 이 설정에 따라 범위가 결정됩니다.
     *
     * 거리 매핑:
     * - VERY_NEAR: 2km (매우 가까운 동네만)
     * - NEAR: 5km (기본값, 인근 동네)
     * - MEDIUM: 20km (중간 거리, 여러 동네)
     * - FAR: 50km (먼 거리, 시/군 단위)
     * - ALL: 20000km (전국, 사실상 제한 없음)
     */
    public enum RangeSetting {
        VERY_NEAR,  // 매우 가까움: 2km
        NEAR,       // 가까움(기본값): 5km
        MEDIUM,     // 중간: 20km
        FAR,        // 먼: 50km
        ALL;        // 전체: 20000km (사실상 제한 없음)

        /**
         * RangeSetting을 실제 거리(km)로 변환합니다.
         *
         * - 각 설정값에 대응하는 킬로미터 값을 반환합니다.
         * - 이 값은 지역 검색 시 반경으로 사용됩니다.
         *
         * @return 거리(킬로미터)
         */
        public double toKilometers() {
            // 각 RangeSetting에 대응하는 거리(km)를 반환합니다.
            switch (this) {
                case VERY_NEAR:
                    return 2.0;    // 매우 가까움: 2km
                case NEAR:
                    return 5.0;    // 가까움: 5km
                case MEDIUM:
                    return 20.0;   // 중간: 20km
                case FAR:
                    return 50.0;   // 먼: 50km
                case ALL:
                    return 20000.0; // 전체: 20000km (지구 둘레의 절반, 사실상 무제한)
                default:
                    return 5.0;    // 기본값: 5km (NEAR와 동일)
            }
        }
    }

    /**
     * 거리 설정을 변경합니다.
     *
     * - 사용자가 활동 반경을 조절할 때 이 메서드를 호출합니다.
     * - null이 입력되면 기본값(NEAR)으로 설정됩니다.
     *
     * @param newSetting 새로운 거리 설정 (null이면 NEAR로 설정)
     */
    public void changeRangeSetting(RangeSetting newSetting) {
        // null 방어: newSetting이 null이면 기본값(NEAR)을 사용합니다.
        this.rangeSetting = newSetting == null ? RangeSetting.NEAR : newSetting;
    }

    /**
     * 현재 사용자의 활동 반경(km)을 반환합니다.
     *
     * - rangeSetting을 실제 거리로 변환하여 반환합니다.
     * - 지역 검색 API에서 이 값을 사용하여 반경을 결정합니다.
     *
     * @return 활동 반경(킬로미터)
     */
    public double getRangeInKilometers() {
        // rangeSetting이 null이면 기본값(NEAR = 5km)을 사용합니다.
        return (rangeSetting == null ? RangeSetting.NEAR : rangeSetting).toKilometers();
    }

    /**
     * 닉네임을 변경합니다.
     *
     * - 사용자가 프로필 설정에서 닉네임을 수정할 때 이 메서드를 호출합니다.
     * - null이나 공백은 허용되지 않습니다(검증은 서비스 레이어에서 수행).
     *
     * @param newNickname 새로운 닉네임 (null이 아니어야 함)
     */
    public void updateNickname(String newNickname) {
        // 닉네임이 null이거나 빈 문자열이면 변경하지 않습니다.
        if (newNickname != null && !newNickname.trim().isEmpty()) {
            this.nickname = newNickname.trim(); // 앞뒤 공백을 제거하고 저장
        }
    }
}
