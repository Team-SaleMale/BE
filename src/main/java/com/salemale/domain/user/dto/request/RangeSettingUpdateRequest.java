package com.salemale.domain.user.dto.request; // 활동 반경 설정 변경 요청 DTO

import com.salemale.domain.user.entity.User; // User 엔티티의 RangeSetting enum 사용
import io.swagger.v3.oas.annotations.media.Schema; // Swagger: 스키마 설명
import jakarta.validation.constraints.NotNull; // null 검증
import lombok.AllArgsConstructor; // Lombok: 모든 필드를 받는 생성자 자동 생성
import lombok.Getter; // Lombok: getter 자동 생성
import lombok.NoArgsConstructor; // Lombok: 기본 생성자 자동 생성

/**
 * RangeSettingUpdateRequest: 사용자의 활동 반경 설정 변경 요청을 담는 DTO입니다.
 *
 * - 사용자가 프로필 설정에서 활동 반경을 조절할 때 사용됩니다.
 * - 가능한 값: VERY_NEAR, NEAR, MEDIUM, FAR, ALL
 * - 각 값은 특정 거리(km)로 변환되어 지역 검색 시 사용됩니다.
 *
 * 거리 매핑:
 * - VERY_NEAR: 2km (매우 가까운 동네만)
 * - NEAR: 5km (기본값, 인근 동네)
 * - MEDIUM: 20km (중간 거리, 여러 동네)
 * - FAR: 50km (먼 거리, 시/군 단위)
 * - ALL: 20000km (전국, 사실상 제한 없음)
 *
 * 검증 규칙:
 * - @NotNull: null 값을 거부합니다.
 *
 * 사용 예시:
 * PATCH /api/users/range-setting
 * {
 *   "rangeSetting": "MEDIUM"
 * }
 */
@Getter // getter 메서드를 자동으로 생성합니다.
@NoArgsConstructor // 기본 생성자를 자동으로 생성합니다(JSON 역직렬화에 필요).
@AllArgsConstructor // 모든 필드를 받는 생성자를 자동으로 생성합니다.
public class RangeSettingUpdateRequest {

    /**
     * 새로운 활동 반경 설정
     *
     * - VERY_NEAR, NEAR, MEDIUM, FAR, ALL 중 하나여야 합니다.
     * - null 값은 허용되지 않습니다.
     * - 잘못된 값(예: "INVALID")이 입력되면 HTTP 400 오류가 발생합니다.
     */
    @NotNull(message = "활동 반경 설정은 필수입니다.") // null 값을 거부
    @Schema(
            description = """
                    활동 반경 설정 (거리 제한)
                    
                    **가능한 값:**
                    - VERY_NEAR: 2km (매우 가까운 동네만)
                    - NEAR: 5km (기본값, 인근 동네)
                    - MEDIUM: 20km (중간 거리, 여러 동네)
                    - FAR: 50km (먼 거리, 시/군 단위)
                    - ALL: 20000km (전국, 사실상 제한 없음)
                    
                    **사용 예시:**
                    - VERY_NEAR: 우리 동네만 보고 싶을 때
                    - NEAR: 일반적인 생활권 (기본값)
                    - MEDIUM: 출퇴근 거리까지 확장
                    - FAR: 시/군 전체를 보고 싶을 때
                    - ALL: 전국의 모든 상품을 보고 싶을 때
                    """,
            example = "NEAR",
            allowableValues = {"VERY_NEAR", "NEAR", "MEDIUM", "FAR", "ALL"}
    )
    private User.RangeSetting rangeSetting;
}

