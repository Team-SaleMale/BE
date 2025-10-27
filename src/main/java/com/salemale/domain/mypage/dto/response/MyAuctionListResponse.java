package com.salemale.domain.mypage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAuctionListResponse {

    private List<MyAuctionItemDTO> items;
    private MyAuctionSummaryDTO summary;

    // 페이징 정보
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer size;
    private Boolean hasNext;
    private Boolean hasPrevious;
}