package com.salemale.domain.item.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 찜한 상품 목록 응답 DTO (페이징 정보 포함)
 */
@Schema(description = "찜한 상품 목록 응답 (페이징 포함)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikedItemListResponse {

    @Schema(description = "찜한 상품 목록")
    private List<LikedItemDTO> likedItems;

    @Schema(description = "전체 찜한 상품 개수", example = "45")
    private Long totalElements;

    @Schema(description = "전체 페이지 수", example = "3")
    private Integer totalPages;

    @Schema(description = "현재 페이지 번호", example = "0")
    private Integer currentPage;

    @Schema(description = "페이지 크기", example = "20")
    private Integer size;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private Boolean hasNext;

    @Schema(description = "이전 페이지 존재 여부", example = "false")
    private Boolean hasPrevious;
}