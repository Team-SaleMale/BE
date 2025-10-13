package com.salemale.domain.item.dto.response.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDetailResponse {

    // 상품 기본 정보
    private Long itemId;
    private String title;
    private String name;
    private String description;
    private String category;
    private String itemStatus;

    // 경매 정보
    private AuctionInfoDTO auctionInfo;

    // 판매자 정보
    private SellerInfoDTO sellerInfo;

    // 최고 입찰자 정보 (nullable)
    private HighestBidderDTO highestBidder;

    // 입찰 내역
    private List<BidHistoryDTO> bidHistory;

    // 지역 정보
    private RegionInfoDTO regionInfo;

    // 거래 정보
    private TradeInfoDTO tradeInfo;

    // 이미지 정보
    private List<ItemImageDTO> images;

    // 사용자 상호작용
    private UserInteractionDTO userInteraction;

    // 메타 정보
    private LocalDateTime createdAt;
}