package com.salemale.domain.item.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 찜하기/찜 취소 응답 DTO
 */
@Schema(description = "찜하기/찜 취소 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemLikeResponse {

    @Schema(description = "경매 상품 ID", example = "1")
    private Long itemId;

    @Schema(description = "응답 메시지", example = "찜하기 완료")
    private String message;

    @Schema(description = "현재 찜 상태", example = "true")
    private Boolean isLiked;

    public static ItemLikeResponse of(Long itemId, boolean isLiked) {
        return ItemLikeResponse.builder()
                .itemId(itemId)
                .isLiked(isLiked)
                .message(isLiked ? "찜하기 완료" : "찜 취소 완료")
                .build();
    }
}