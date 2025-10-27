package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.enums.AuctionSortType;
import com.salemale.domain.item.enums.AuctionStatus;
import com.salemale.domain.mypage.enums.MyAuctionSortType;
import com.salemale.domain.mypage.enums.MyAuctionType;
import com.salemale.domain.user.entity.User;
import com.salemale.global.common.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Item Repository QueryDSL 커스텀 인터페이스
 */
public interface ItemRepositoryCustom {

    /**
     * 경매 상품 리스트 조회 (동적 쿼리)
     *
     * @param status 상태 필터 (BIDDING, COMPLETED, POPULAR)
     * @param categories 카테고리 필터, 카테고리 다중 선택 가능
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @param sortType 정렬 타입
     * @param pageable 페이징 정보
     * @return 경매 상품 페이지
     */
    Page<Item> findAuctionList(
            AuctionStatus status,
            List<Category> categories,
            Integer minPrice,
            Integer maxPrice,
            AuctionSortType sortType,
            Pageable pageable
    );

    // 내 경매 목록 조회
    Page<Item> findMyAuctions(
            User user,
            MyAuctionType type,
            MyAuctionSortType sortType,
            Pageable pageable
    );
}