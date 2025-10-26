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
public class BidHistoryDTO {

    private Long transactionId;

    // 입찰자 정보 (간략)
    private BidderInfoDTO bidder;

    private Integer bidPrice;
    private LocalDateTime bidTime;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BidderInfoDTO {
        private Long userId;
        private String nickname;
        private String profileImage;
    }
}