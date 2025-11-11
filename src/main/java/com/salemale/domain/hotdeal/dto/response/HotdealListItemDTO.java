package com.salemale.domain.hotdeal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 핫딜 상품 리스트 항목 DTO
 * - 지도에 표시하기 위한 위도/경도 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotdealListItemDTO {

    private Long itemId;

    // 상품 정보
    private String name;              // 상품명 (예: "아삭 문패인/케일 믹스 1kg")
    private List<String> imageUrls;
    private Integer currentPrice;
    private Integer startPrice;
    private Long bidderCount;
    private LocalDateTime endTime;
    private String itemStatus;

    // 가게 정보
    private Long storeId;
    private String storeName;         // 가게명 (예: "제소마실")
    private Double latitude;          // 🔥 지도 마커용
    private Double longitude;         // 🔥 지도 마커용
    private String address;

    private LocalDateTime createdAt;
}