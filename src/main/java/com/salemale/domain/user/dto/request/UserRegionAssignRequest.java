package com.salemale.domain.user.dto.request; // 유저-지역 할당 요청 DTO

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UserRegionAssignRequest: 사용자에게 지역을 할당하는 요청 DTO
 * 
 * - sido, sigungu, eupmyeondong을 모두 포함하여 명확한 지역 식별을 보장합니다.
 * - 동일한 이름의 동네가 여러 시/군/구에 존재할 수 있으므로 모든 정보가 필요합니다.
 * 
 * 예시:
 * - sido: "서울특별시", sigungu: "관악구", eupmyeondong: "신림동"
 * - sido: "경기도", sigungu: "성남시", eupmyeondong: "분당동"
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegionAssignRequest {
    
    /**
     * 시/도 (예: "서울특별시", "경기도", "부산광역시")
     */
    @NotBlank(message = "시/도는 필수입니다.")
    @Size(max = 50, message = "시/도는 최대 50자입니다.")
    private String sido;
    
    /**
     * 시/군/구 (예: "관악구", "성남시", "해운대구")
     */
    @NotBlank(message = "시/군/구는 필수입니다.")
    @Size(max = 50, message = "시/군/구는 최대 50자입니다.")
    private String sigungu;
    
    /**
     * 읍/면/동 (예: "신림동", "분당동", "우동")
     */
    @NotBlank(message = "읍/면/동은 필수입니다.")
    @Size(max = 50, message = "읍/면/동은 최대 50자입니다.")
    private String eupmyeondong;

    /**
     * 주 활동 동네 여부 (현재는 항상 true)
     */
    private boolean primary = true;
}
