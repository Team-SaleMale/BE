package com.salemale.domain.hotdeal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 핫딜 상품 리스트 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotdealListResponse {

    private List<HotdealListItemDTO> items;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer size;
    private Boolean hasNext;
    private Boolean hasPrevious;
}