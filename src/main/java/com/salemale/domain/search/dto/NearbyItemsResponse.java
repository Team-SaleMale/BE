package com.salemale.domain.search.dto;

import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NearbyItemsResponse {
    private final List<AuctionListItemDTO> items;
    private final long totalElements;
    private final int totalPages;
    private final int currentPage;
    private final int size;
    private final boolean hasNext;
    private final boolean hasPrevious;
}


