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
        // 상태 매핑: COMPLETED은 SUCCESS/FAIL 모두를 포함하는 별도 요구가 있으나, 검색/노출 관점에서는 우선 SUCCESS/FAIL을 COMPLETED로 다루는 리스트 API에 맡기고
        // 키워드 검색은 우선 BIDDING에 한정. POPULAR/RECOMMENDED도 리스트 전용 처리 대상이므로 여기서는 BIDDING 기본값 적용.
        com.salemale.global.common.enums.ItemStatus effectiveStatus = ItemStatus.BIDDING;

        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        UserRegion primary = userRegionRepository.findByPrimaryUser(user)
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_SET));

        Double effective = user.getRangeInKilometers();
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
            // 전국 검색: JPQL + 옵션 필터 + 동적 정렬(Pageable Sort)
            java.util.List<Category> cats = (categories == null || categories.isEmpty()) ? null : categories;
            // Pageable에 정렬 매핑 적용
            org.springframework.data.domain.Pageable sorted = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    toSpringSort(sort)
            );
            page = itemRepository.searchItemsByKeywordWithFilters(
                    effectiveStatus, keyword, cats, normalizeMin(minPrice), normalizeMax(maxPrice), sorted
            );
        } else {
            // 반경 검색: 기존 네이티브(키워드+반경) → 이후 카테고리/가격/정렬은 메모리에서 보정
            page = itemRepository.findNearbyItemsByKeyword(effectiveStatus.name(), keyword, lat, lon, km, pageable);
            java.util.List<Item> filtered = page.getContent().stream()
                    .filter(it -> categories == null || categories.isEmpty() || categories.contains(it.getCategory()))
                    .filter(it -> minPrice == null || minPrice == 0 || it.getCurrentPrice() >= minPrice)
                    .filter(it -> maxPrice == null || maxPrice == 0 || it.getCurrentPrice() <= maxPrice)
                    .sorted(java.util.Comparator.comparing((Item it) -> 0))
                    .toList();
            // 정렬 보정
            java.util.Comparator<Item> comp = comparatorFor(sort);
            if (comp != null) {
                filtered = filtered.stream().sorted(comp).toList();
            }
            // 페이지네이션 유지: 이미 DB 페이징이 되어 있으므로, 정렬만 보정하여 DTO 매핑
            return new org.springframework.data.domain.PageImpl<>(
                    filtered.stream().map(ItemConverter::toAuctionListItemDTO).toList(),
                    pageable,
                    page.getTotalElements()
            );
        }
        return page.map(ItemConverter::toAuctionListItemDTO);
    }

    private Integer normalizeMin(Integer min) { return (min != null && min > 0) ? min : null; }
    private Integer normalizeMax(Integer max) { return (max != null && max > 0) ? max : null; }

    private org.springframework.data.domain.Sort toSpringSort(AuctionSortType sort) {
        if (sort == null) return org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt"));
        return switch (sort) {
            case CREATED_DESC -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt"));
            case PRICE_ASC -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("currentPrice"));
            case PRICE_DESC -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("currentPrice"));
            case VIEW_COUNT_DESC -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("viewCount"));
            case END_TIME_ASC -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.asc("endTime"));
            case BID_COUNT_DESC -> org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("bidCount"));
        };
    }

    private java.util.Comparator<Item> comparatorFor(AuctionSortType sort) {
        if (sort == null) return java.util.Comparator.comparing(Item::getCreatedAt).reversed();
        return switch (sort) {
            case CREATED_DESC -> java.util.Comparator.comparing(Item::getCreatedAt).reversed();
            case PRICE_ASC -> java.util.Comparator.comparing(Item::getCurrentPrice);
            case PRICE_DESC -> java.util.Comparator.comparing(Item::getCurrentPrice).reversed();
            case VIEW_COUNT_DESC -> java.util.Comparator.comparing(Item::getViewCount).reversed();
            case END_TIME_ASC -> java.util.Comparator.comparing(Item::getEndTime);
            case BID_COUNT_DESC -> java.util.Comparator.comparing(Item::getBidCount).reversed();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuctionListItemDTO> searchCompletedItems(String q, Pageable pageable) {
        if (q == null || q.trim().isBlank()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        String keyword = q.trim();

        // 낙찰된 상품만 조회 (ItemStatus.SUCCESS), 날짜순 정렬 (최신순)
        ItemStatus status = ItemStatus.SUCCESS;
        Page<Item> page = itemRepository.searchItemsByKeyword(status, keyword, pageable);

        return page.map(ItemConverter::toAuctionListItemDTO);
    }
}


