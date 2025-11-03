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
public class NearbyItemSearchServiceImpl implements NearbyItemSearchService {

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuctionListItemDTO> findNearbyItemsForUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        UserRegion primary = userRegionRepository.findByPrimaryUser(user)
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_SET));

        double km = user.getRangeInKilometers();
        double lat = primary.getRegion().getLatitude().doubleValue();
        double lon = primary.getRegion().getLongitude().doubleValue();

        Page<Item> page = itemRepository.findNearbyItems(ItemStatus.BIDDING.name(), lat, lon, km, pageable);
        return page.map(ItemConverter::toAuctionListItemDTO);
    }
}


