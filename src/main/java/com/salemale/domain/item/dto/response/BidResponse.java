package com.salemale.domain.item.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {

    private Long transactionId;          // 입찰 거래 ID
    private Long itemId;                 // 상품 ID
    private Long buyerId;                // 입찰자 ID
    private Integer bidPrice;            // 입찰한 가격
    private Integer previousPrice;       // 입찰 전 최고가
    private Integer currentHighestPrice; // 입찰 후 최고가 (= bidPrice)
    private Integer bidIncrement;        // 최소 입찰 단위
    private Long bidCount;               // 총 입찰 수
    private LocalDateTime bidTime;       // 입찰 시각
    private LocalDateTime endTime;       // 경매 종료 시간
}