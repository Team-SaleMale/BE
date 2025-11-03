package com.salemale.domain.search.service;

import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KeywordItemSearchService {
    Page<AuctionListItemDTO> search(Long userId, String q, boolean includeOutside, Double distanceKmOverride, Pageable pageable);
}


