package com.salemale.domain.item.dto.response.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionInfoDTO {
    private Integer startPrice;
    private Integer currentPrice;
    private Integer bidIncrement;
    private LocalDateTime endTime;
    private Long bidCount;
    private Long viewCount;
}