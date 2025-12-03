package com.salemale.domain.item.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 초기 가격 추천 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드는 JSON에 포함하지 않음
public class PriceSuggestionResponse {

    /**
     * 추천 시작가 (null일 경우 데이터 없음)
     */
    private Integer recommendedPrice;

    /**
     * 설명 메시지
     */
    private String message;

    /**
     * 데이터 사용 가능 여부
     */
    @Builder.Default
    private Boolean dataAvailable = true;

    /**
     * 데이터가 없을 때 사용하는 팩토리 메서드
     */
    public static PriceSuggestionResponse noData() {
        return PriceSuggestionResponse.builder()
                .recommendedPrice(null)
                .message("시세 데이터를 찾을 수 없습니다")
                .dataAvailable(false)
                .build();
    }

    /**
     * 타임아웃 발생 시 사용하는 팩토리 메서드
     */
    public static PriceSuggestionResponse timeout() {
        return PriceSuggestionResponse.builder()
                .recommendedPrice(null)
                .message("시세 조회 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.")
                .dataAvailable(false)
                .build();
    }

    /**
     * 에러 발생 시 사용하는 팩토리 메서드
     */
    public static PriceSuggestionResponse error(String errorMessage) {
        return PriceSuggestionResponse.builder()
                .recommendedPrice(null)
                .message(errorMessage != null ? errorMessage : "시세 조회 중 오류가 발생했습니다")
                .dataAvailable(false)
                .build();
    }
}
