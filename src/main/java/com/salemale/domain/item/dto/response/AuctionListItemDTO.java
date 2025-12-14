package com.salemale.domain.item.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경매 상품 리스트 항목 DTO
 */
@Schema(description = "경매 상품 리스트 항목 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionListItemDTO {

    @Schema(description = "경매 상품 ID", example = "123")
    private Long itemId;

    @Schema(description = "상품 제목", example = "루테인 지아잔틴 (60캡슐)")
    private String title;

    @Schema(description = "해당 경매상품의 모든 이미지 URL")
    private List<String> imageUrls;      // 모든 이미지 URL

    @Schema(description = "현재 입찰가", example = "850000")
    private Integer currentPrice;

    @Schema(description = "현재 입찰자 수", example = "100")
    private Long bidderCount;

    @Schema(description = "경매 종료 시간", example = "2025-10-16T23:56:29")
    private LocalDateTime endTime;

    @Schema(description = "조회수", example = "1944")
    private Long viewCount;

    @Schema(description = "상품 상태", example = "BIDDING")
    private String itemStatus;

    @Schema(description = "경매 시작가")
    private Integer startPrice;          // 시작가 추가

    @Schema(description = "경매 시작 날짜")
    private LocalDateTime createdAt;     // 생성일(경매 시작날짜) 추가

    // 로그인 사용자가 이 판매자를 차단했는지 여부
    @Schema(description = "차단한 판매자의 상품인지 여부", example = "false")
    private boolean blockedSeller;
}
