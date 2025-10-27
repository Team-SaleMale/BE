package com.salemale.domain.item.enums;

// 경매 상품 리스트 페이지 상태 필터(추후 추천 페이지도 추가 예정)

public enum AuctionStatus {
    BIDDING,    // 진행중 (입찰 가능)
    COMPLETED,  // 진행완료 (낙찰/유찰)
    POPULAR     // 인기 (최근 3일 내 입찰 많은 상품)
}
