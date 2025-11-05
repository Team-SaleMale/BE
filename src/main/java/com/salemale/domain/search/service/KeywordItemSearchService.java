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
            Long userId,
            String q,
            User.RangeSetting radius,
            AuctionStatus status,
            java.util.List<Category> categories,
            Integer minPrice,
            Integer maxPrice,
            AuctionSortType sort,
            Pageable pageable
    );
}


