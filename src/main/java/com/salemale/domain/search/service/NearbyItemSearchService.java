package com.salemale.domain.search.service;

import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NearbyItemSearchService {
    Page<AuctionListItemDTO> findNearbyItemsForUser(Long userId, Pageable pageable);
}


