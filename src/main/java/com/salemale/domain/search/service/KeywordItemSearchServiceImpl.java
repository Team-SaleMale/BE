package com.salemale.domain.search.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.item.converter.ItemConverter;
import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.entity.UserRegion;
import com.salemale.domain.user.repository.UserRegionRepository;
import com.salemale.domain.user.repository.UserRepository;
import com.salemale.global.common.enums.ItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KeywordItemSearchServiceImpl implements KeywordItemSearchService {

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuctionListItemDTO> search(Long userId, String q, boolean includeOutside, Double distanceKmOverride, Pageable pageable) {
        if (q == null || q.trim().isBlank()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        String keyword = q.trim();

        if (includeOutside) {
            Page<Item> page = itemRepository.searchItemsByKeyword(ItemStatus.BIDDING, keyword, pageable);
            return page.map(ItemConverter::toAuctionListItemDTO);
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        UserRegion primary = userRegionRepository.findByPrimaryUser(user)
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_SET));

        double km = distanceKmOverride != null ? Math.max(distanceKmOverride, 0.1) : user.getRangeInKilometers();
        double lat = primary.getRegion().getLatitude().doubleValue();
        double lon = primary.getRegion().getLongitude().doubleValue();

        Page<Item> page = itemRepository.findNearbyItemsByKeyword(ItemStatus.BIDDING.name(), keyword, lat, lon, km, pageable);
        return page.map(ItemConverter::toAuctionListItemDTO);
    }
}


