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
import com.salemale.domain.user.repository.BlockListRepository; // ADD 차단 필터용
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NearbyItemSearchServiceImpl implements NearbyItemSearchService {

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final ItemRepository itemRepository;
    private final BlockListRepository blockListRepository; // ADD 차단 필터용

    @Override
    @Transactional(readOnly = true)
    public Page<AuctionListItemDTO> findNearbyItemsForUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        UserRegion primary = userRegionRepository.findByPrimaryUser(user)
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_SET));

        double km = user.getRangeInKilometers(); // 기본값 보장(NEAR=5km)
        double lat = primary.getRegion().getLatitude().doubleValue();
        double lon = primary.getRegion().getLongitude().doubleValue();

        // ADD 내가 차단한 사용자 ID 목록
        List<Long> blockedUserIds = blockListRepository.findBlockedUserIds(userId);

        Page<Item> page = itemRepository.findNearbyItems(ItemStatus.BIDDING.name(), lat, lon, km, pageable);

        // ADD 차단 유저 필터 적용
        if (!blockedUserIds.isEmpty()) {
            page = new org.springframework.data.domain.PageImpl<>(
                    page.getContent().stream()
                            .filter(it -> !blockedUserIds.contains(it.getSeller().getId()))
                            .toList(),
                    pageable,
                    page.getTotalElements()
            );
        }

        return page.map(ItemConverter::toAuctionListItemDTO);
    }
}


