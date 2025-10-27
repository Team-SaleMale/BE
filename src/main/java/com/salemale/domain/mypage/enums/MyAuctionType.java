package com.salemale.domain.mypage.enums;

public enum MyAuctionType {
    ALL,        // 전체 (판매 + 입찰)
    SELLING,    // 판매 (내가 판매자)
    BIDDING,    // 입찰 (내가 입찰한 상품)
    WON,        // 낙찰 (내가 낙찰받은 상품)
    FAILED      // 유찰 (내 상품이 유찰됨)
}