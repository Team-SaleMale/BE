package com.salemale.domain.mypage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 찜한 상품 개별 항목 DTO
 */
@Schema(description = "찜한 상품 개별 항목 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikedItemDTO {

    @Schema(description = "경매 상품 ID", example = "123")
    private Long itemId;

    @Schema(description = "상품 제목", example = "루테인 지아잔틴 (60캡슐)")
    private String title;

    @Schema(description = "썸네일 이미지 URL", example = "https://example.com/images/item123.jpg")
    private String thumbnailUrl;

    @Schema(description = "현재 입찰자 수", example = "100")
    private Long bidderCount;

    @Schema(description = "경매 종료 시간 (남은 시간 계산용)", example = "2025-10-16T23:56:29")
    private LocalDateTime endTime;

    @Schema(description = "조회수", example = "1944")
    private Long viewCount;

    @Schema(description = "경매 시작 가격", example = "100000")
    private Integer startPrice;

    @Schema(description = "현재 입찰 가격", example = "150000")
    private Integer currentPrice;
}