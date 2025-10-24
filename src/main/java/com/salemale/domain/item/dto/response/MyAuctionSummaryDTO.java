package com.salemale.domain.item.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAuctionSummaryDTO {

    private Long totalCount;      // 전체
    private Long sellingCount;    // 판매
    private Long biddingCount;    // 입찰
    private Long wonCount;        // 낙찰
    private Long failedCount;     // 유찰
}