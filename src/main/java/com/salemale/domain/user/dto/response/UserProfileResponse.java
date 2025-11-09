package com.salemale.domain.user.dto.response; // 사용자 프로필 응답 DTO

import com.salemale.domain.region.dto.response.RegionInfoDTO; // 지역 정보 DTO
import com.salemale.domain.user.entity.User; // 사용자 엔티티
import lombok.AllArgsConstructor; // Lombok: 모든 필드를 받는 생성자 자동 생성
import lombok.Builder; // Lombok: 빌더 패턴 자동 생성
import lombok.Getter; // Lombok: getter 자동 생성

/**
 * UserProfileResponse: 사용자 프로필 정보를 클라이언트에 전달하는 DTO입니다.
 *
 * - 사용자의 기본 프로필 정보를 포함합니다.
 * - 민감한 정보(비밀번호 등)는 포함하지 않습니다.
 * - 프로필 조회 API의 응답으로 사용됩니다.
 *
 * 포함 정보:
 * - 사용자 ID
 * - 닉네임
 * - 이메일
 * - 매너 점수
 * - 활동 반경 설정
 * - 프로필 이미지 URL
 * - 알림 설정
 * - 전화번호
 * - 전화번호 인증 완료 여부
 * - 주 활동 동네 정보 (지역 정보, null 가능)
 */
@Getter // 모든 필드에 대한 getter 메서드를 자동으로 생성합니다.
@Builder // 빌더 패턴을 사용하여 객체를 생성할 수 있게 합니다.
@AllArgsConstructor // 모든 필드를 매개변수로 받는 생성자를 자동으로 생성합니다.
public class UserProfileResponse {

    private Long id; // 사용자 고유 ID
    private String nickname; // 닉네임 (UI에 표시되는 별칭)
    private String email; // 이메일 (null 가능, 소셜 로그인 시)
    private Integer mannerScore; // 매너 점수 (사용자 신뢰도 지표)
    private String rangeSetting; // 활동 반경 설정 (VERY_NEAR, NEAR, MEDIUM, FAR, ALL)
    private String profileImage; // 프로필 이미지 URL
    private String alarmChecked; // 알림 허용 여부 (YES, NO)
    private String phoneNumber; // 전화번호
    private Boolean phoneVerified; // 전화번호 인증 완료 여부
    private RegionInfoDTO primaryRegion; // 주 활동 동네 정보 (null 가능, 지역 미설정 시)

    /**
     * User 엔티티를 UserProfileResponse로 변환하는 정적 팩토리 메서드입니다.
     *
     * - 엔티티의 필드를 DTO 필드로 매핑합니다.
     * - Enum 타입은 문자열로 변환하여 클라이언트에 전달합니다.
     * - phoneVerifiedAt이 null이 아니면 인증 완료로 간주합니다.
     * - 지역 정보는 별도로 조회하여 포함해야 합니다 (이 메서드에서는 null로 설정).
     *
     * @param user User 엔티티
     * @return UserProfileResponse DTO
     */
    public static UserProfileResponse from(User user) {
        return from(user, null);
    }

    /**
     * User 엔티티와 지역 정보를 UserProfileResponse로 변환하는 정적 팩토리 메서드입니다.
     *
     * - 엔티티의 필드를 DTO 필드로 매핑합니다.
     * - Enum 타입은 문자열로 변환하여 클라이언트에 전달합니다.
     * - phoneVerifiedAt이 null이 아니면 인증 완료로 간주합니다.
     * - 지역 정보를 포함합니다 (null 가능).
     *
     * @param user User 엔티티
     * @param primaryRegion 주 활동 동네 정보 (null 가능)
     * @return UserProfileResponse DTO
     */
    public static UserProfileResponse from(User user, RegionInfoDTO primaryRegion) {
        // 엔티티가 null이면 null을 반환합니다.
        if (user == null) return null;

        // 빌더 패턴을 사용하여 DTO를 생성합니다.
        return UserProfileResponse.builder()
                .id(user.getId()) // 사용자 ID
                .nickname(user.getNickname()) // 닉네임
                .email(user.getEmail()) // 이메일 (null 가능)
                .mannerScore(user.getMannerScore()) // 매너 점수
                .rangeSetting(user.getRangeSetting() != null ? user.getRangeSetting().name() : null) // RangeSetting을 문자열로 변환
                .profileImage(user.getProfileImage()) // 프로필 이미지 URL
                .alarmChecked(user.getAlarmChecked() != null ? user.getAlarmChecked().name() : null) // AlarmChecked를 문자열로 변환
                .phoneNumber(user.getPhoneNumber()) // 전화번호
                .phoneVerified(user.getPhoneVerifiedAt() != null) // 인증 시각이 있으면 true
                .primaryRegion(primaryRegion) // 주 활동 동네 정보 (null 가능)
                .build();
    }
}

