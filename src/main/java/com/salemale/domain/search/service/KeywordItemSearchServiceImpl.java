package com.salemale.domain.search.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.item.converter.ItemConverter;
import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.enums.AuctionSortType;
import com.salemale.domain.item.enums.AuctionStatus;
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.entity.UserRegion;
import com.salemale.domain.user.repository.UserRegionRepository;
import com.salemale.domain.user.repository.UserRepository;
import com.salemale.global.common.enums.ItemStatus;
import com.salemale.global.common.enums.Category;
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
    public Page<AuctionListItemDTO> search(Long userId, String q, User.RangeSetting radius, AuctionStatus status, java.util.List<Category> categories, Integer minPrice, Integer maxPrice, AuctionSortType sort, Pageable pageable) {
        if (q == null || q.trim().isBlank()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        String keyword = q.trim();

        boolean nationwide = (radius != null && radius == User.RangeSetting.ALL);
        // 상태는 현재 BIDDING만 지원. 다른 상태는 BIDDING으로 디폴트 처리
        com.salemale.global.common.enums.ItemStatus effectiveStatus = ItemStatus.BIDDING;

        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        UserRegion primary = userRegionRepository.findByPrimaryUser(user)
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_SET));

        Double effective = nationwide ? user.getRangeInKilometers() : user.getRangeInKilometers();
        if (radius != null && radius != User.RangeSetting.ALL) {
            effective = Math.max(radius.toKilometers(), 0.1);
        }
        if (effective == null || effective <= 0) {
            throw new GeneralException(ErrorStatus.USER_REGION_NOT_SET);
        }
        double km = effective;
        double lat = primary.getRegion().getLatitude().doubleValue();
        double lon = primary.getRegion().getLongitude().doubleValue();

        Page<Item> page;
        if (nationwide) {
            page = itemRepository.searchItemsByKeyword(effectiveStatus, keyword, pageable);
        } else {
            page = itemRepository.findNearbyItemsByKeyword(effectiveStatus.name(), keyword, lat, lon, km, pageable);
        }
        return page.map(ItemConverter::toAuctionListItemDTO);
    }
}


