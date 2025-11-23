package com.salemale.domain.search.service;

import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import com.salemale.domain.item.enums.AuctionSortType;
import com.salemale.domain.item.enums.AuctionStatus;
import com.salemale.domain.user.entity.User;
import com.salemale.global.common.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KeywordItemSearchService {
    Page<AuctionListItemDTO> search(
            java.util.Optional<Long> userId,
            String q,
            User.RangeSetting radius,
            AuctionStatus status,
            java.util.List<Category> categories,
            Integer minPrice,
            Integer maxPrice,
            AuctionSortType sort,
            Pageable pageable
    );

    /**
     * 중고 시세 검색: 낙찰된 상품(ItemStatus.SUCCESS)만 키워드로 조회
     * 
     * @param q 검색 키워드
     * @param pageable 페이징 정보
     * @return 낙찰된 상품 목록 (날짜순 정렬)
     */
    Page<AuctionListItemDTO> searchCompletedItems(
            String q,
            Pageable pageable
    );
}


