package com.salemale.domain.mypage.dto.response;

import com.salemale.domain.mypage.enums.MyRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAuctionItemDTO {

    // 기본 정보
    private Long itemId;
    private String title;
    private String thumbnailUrl;

    // 통계 정보
    private Long viewCount;
    private Long bidderCount;

    // 가격 정보
    private Integer startPrice;
    private Integer currentPrice;

    // 상태 정보
    private String itemStatus;         // BIDDING, SUCCESS, FAIL
    private LocalDateTime endTime;     // 프론트에서 남은 시간 계산

    // 내 역할
    private MyRole myRole;             // SELLER, BIDDER, WINNER
    private Boolean isHighestBidder;   // 내가 현재 최고가 입찰자인지
}