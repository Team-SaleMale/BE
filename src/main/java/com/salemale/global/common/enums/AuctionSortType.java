package com.salemale.global.common.enums;

// 경매 상품 리스트 상품 정렬 타입
public enum AuctionSortType {
    CREATED_DESC,      // 최신순 (기본값)
    BID_COUNT_DESC,    // 입찰 많은순
    PRICE_ASC,         // 가격 낮은순
    PRICE_DESC,        // 가격 높은순
    VIEW_COUNT_DESC,   // 조회수 많은순
    END_TIME_ASC       // 마감 임박순
}
