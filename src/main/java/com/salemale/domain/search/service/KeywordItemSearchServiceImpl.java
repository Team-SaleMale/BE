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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KeywordItemSearchServiceImpl implements KeywordItemSearchService {

    private final UserRepository userRepository;
    private final UserRegionRepository userRegionRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuctionListItemDTO> search(java.util.Optional<Long> userIdOpt, String q, User.RangeSetting radius, AuctionStatus status, java.util.List<Category> categories, Integer minPrice, Integer maxPrice, AuctionSortType sort, Pageable pageable) {
        // q가 null이거나 비어있으면 키워드 검색 없이 필터만 적용
        String keyword = (q != null && !q.trim().isBlank()) ? q.trim() : null;

        // 비로그인 사용자: 전체 지역 표시로 전국 검색
        if (userIdOpt.isEmpty()) {
            // 비로그인 사용자는 radius 파라미터를 무시하고 항상 전국 검색
            java.util.List<Category> cats = (categories == null || categories.isEmpty()) ? null : categories;
            org.springframework.data.domain.Pageable sorted = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    toSpringSort(sort)
            );
            
            // COMPLETED 상태는 SUCCESS와 FAIL 둘 다 포함
            if (isCompletedStatus(status)) {
                Page<Item> page = searchCompletedItems(keyword, cats, normalizeMin(minPrice), normalizeMax(maxPrice), sorted);
                return page.map(ItemConverter::toAuctionListItemDTO);
            }
            
            com.salemale.global.common.enums.ItemStatus effectiveStatus = mapAuctionStatusToItemStatus(status);
            boolean isPopular = (status != null && status == AuctionStatus.POPULAR);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threeDaysAgo = now.minusDays(3);
            
            if (keyword != null) {
                // 키워드가 있으면 키워드 검색 + 필터
                Page<Item> page = itemRepository.searchItemsByKeywordWithFilters(
                        effectiveStatus, keyword, cats, normalizeMin(minPrice), normalizeMax(maxPrice),
                        isPopular, threeDaysAgo, now, sorted
                );
                return page.map(ItemConverter::toAuctionListItemDTO);
            } else {
                // 키워드가 없으면 필터만 적용 (키워드 조건 제외)
                Page<Item> page = itemRepository.searchItemsByFiltersOnly(
                        effectiveStatus, cats, normalizeMin(minPrice), normalizeMax(maxPrice),
                        isPopular, threeDaysAgo, now, sorted
                );
                return page.map(ItemConverter::toAuctionListItemDTO);
            }
        }

        // 로그인 사용자: 기존 로직 (지역 기반 검색)
        Long userId = userIdOpt.get();
        boolean nationwide = (radius != null && radius == User.RangeSetting.ALL);

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

        // COMPLETED 상태는 SUCCESS와 FAIL 둘 다 포함
        if (isCompletedStatus(status)) {
            java.util.List<Category> cats = (categories == null || categories.isEmpty()) ? null : categories;
            org.springframework.data.domain.Pageable sorted = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    toSpringSort(sort)
            );
            
            Page<Item> page = nationwide 
                    ? searchCompletedItems(keyword, cats, normalizeMin(minPrice), normalizeMax(maxPrice), sorted)
                    : searchCompletedItemsNearby(keyword, lat, lon, km, cats, normalizeMin(minPrice), normalizeMax(maxPrice), sort, pageable);
            return page.map(ItemConverter::toAuctionListItemDTO);
        }

        com.salemale.global.common.enums.ItemStatus effectiveStatus = mapAuctionStatusToItemStatus(status);
        boolean isPopular = (status != null && status == AuctionStatus.POPULAR);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysAgo = now.minusDays(3);
        
        Page<Item> page;
        if (nationwide) {
            // 전국 검색
            java.util.List<Category> cats = (categories == null || categories.isEmpty()) ? null : categories;
            // Pageable에 정렬 매핑 적용
            org.springframework.data.domain.Pageable sorted = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    toSpringSort(sort)
            );
            
            if (keyword != null) {
                // 키워드가 있으면 키워드 검색 + 필터
                page = itemRepository.searchItemsByKeywordWithFilters(
                        effectiveStatus, keyword, cats, normalizeMin(minPrice), normalizeMax(maxPrice),
                        isPopular, threeDaysAgo, now, sorted
                );
            } else {
                // 키워드가 없으면 필터만 적용 (키워드 조건 제외)
                page = itemRepository.searchItemsByFiltersOnly(
                        effectiveStatus, cats, normalizeMin(minPrice), normalizeMax(maxPrice),
                        isPopular, threeDaysAgo, now, sorted
                );
            }
        } else {
            // 반경 검색
            if (keyword != null) {
                // 키워드가 있으면 키워드 + 반경 검색
                page = itemRepository.findNearbyItemsByKeyword(effectiveStatus.name(), keyword, lat, lon, km, pageable);
            } else {
                // 키워드가 없으면 반경 검색만 (키워드 조건 제외)
                page = itemRepository.findNearbyItems(effectiveStatus.name(), lat, lon, km, pageable);
            }
            
            // 반경 검색의 경우 카테고리/가격 필터 및 POPULAR 조건은 메모리에서 보정
            java.util.List<Item> filtered = page.getContent().stream()
                    .filter(it -> categories == null || categories.isEmpty() || categories.contains(it.getCategory()))
                    .filter(it -> minPrice == null || minPrice == 0 || it.getCurrentPrice() >= minPrice)
                    .filter(it -> maxPrice == null || maxPrice == 0 || it.getCurrentPrice() <= maxPrice)
                    .filter(it -> !isPopular || (
                        it.getBidCount() >= 3
                        && it.getCreatedAt().isAfter(threeDaysAgo)
                        && it.getEndTime().isAfter(now)
                    ))
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

    /**
     * AuctionStatus를 ItemStatus로 매핑
     */
    private com.salemale.global.common.enums.ItemStatus mapAuctionStatusToItemStatus(AuctionStatus status) {
        if (status == null) {
            return ItemStatus.BIDDING; // 기본값
        }
        return switch (status) {
            case BIDDING -> ItemStatus.BIDDING;
            case COMPLETED -> null; // COMPLETED는 별도 처리
            case POPULAR -> ItemStatus.BIDDING; // 인기도 진행중 상품만
            case RECOMMENDED -> ItemStatus.BIDDING; // 추천도 진행중 상품만
        };
    }

    /**
     * COMPLETED 상태인지 확인
     */
    private boolean isCompletedStatus(AuctionStatus status) {
        return status != null && status == AuctionStatus.COMPLETED;
    }

    /**
     * COMPLETED 상태 상품 검색 (SUCCESS와 FAIL 둘 다 포함, 전국 검색)
     */
    private Page<Item> searchCompletedItems(String keyword, java.util.List<Category> categories, Integer minPrice, Integer maxPrice, org.springframework.data.domain.Pageable pageable) {
        // SUCCESS와 FAIL 둘 다 조회 (COMPLETED는 POPULAR 조건 적용 안 함)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysAgo = now.minusDays(3);
        Page<Item> successPage = keyword != null
                ? itemRepository.searchItemsByKeywordWithFilters(ItemStatus.SUCCESS, keyword, categories, minPrice, maxPrice, false, threeDaysAgo, now, pageable)
                : itemRepository.searchItemsByFiltersOnly(ItemStatus.SUCCESS, categories, minPrice, maxPrice, false, threeDaysAgo, now, pageable);
        
        Page<Item> failPage = keyword != null
                ? itemRepository.searchItemsByKeywordWithFilters(ItemStatus.FAIL, keyword, categories, minPrice, maxPrice, false, threeDaysAgo, now, pageable)
                : itemRepository.searchItemsByFiltersOnly(ItemStatus.FAIL, categories, minPrice, maxPrice, false, threeDaysAgo, now, pageable);
        
        // 두 결과를 합치기
        java.util.List<Item> combined = new java.util.ArrayList<>();
        combined.addAll(successPage.getContent());
        combined.addAll(failPage.getContent());
        
        // 정렬 적용
        combined.sort(java.util.Comparator.comparing(Item::getCreatedAt).reversed());
        
        // 페이징 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combined.size());
        java.util.List<Item> pagedContent = combined.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(
                pagedContent,
                pageable,
                combined.size()
        );
    }

    /**
     * COMPLETED 상태 상품 검색 (SUCCESS와 FAIL 둘 다 포함, 반경 검색)
     */
    private Page<Item> searchCompletedItemsNearby(String keyword, double lat, double lon, double km, java.util.List<Category> categories, Integer minPrice, Integer maxPrice, AuctionSortType sort, Pageable pageable) {
        // SUCCESS와 FAIL 둘 다 조회
        Page<Item> successPage = keyword != null
                ? itemRepository.findNearbyItemsByKeyword(ItemStatus.SUCCESS.name(), keyword, lat, lon, km, pageable)
                : itemRepository.findNearbyItems(ItemStatus.SUCCESS.name(), lat, lon, km, pageable);
        
        Page<Item> failPage = keyword != null
                ? itemRepository.findNearbyItemsByKeyword(ItemStatus.FAIL.name(), keyword, lat, lon, km, pageable)
                : itemRepository.findNearbyItems(ItemStatus.FAIL.name(), lat, lon, km, pageable);
        
        // 두 결과를 합치기
        java.util.List<Item> combined = new java.util.ArrayList<>();
        combined.addAll(successPage.getContent());
        combined.addAll(failPage.getContent());
        
        // 카테고리/가격 필터 적용
        combined = combined.stream()
                .filter(it -> categories == null || categories.isEmpty() || categories.contains(it.getCategory()))
                .filter(it -> minPrice == null || minPrice == 0 || it.getCurrentPrice() >= minPrice)
                .filter(it -> maxPrice == null || maxPrice == 0 || it.getCurrentPrice() <= maxPrice)
                .toList();
        
        // 정렬 적용
        java.util.Comparator<Item> comp = comparatorFor(sort);
        if (comp != null) {
            combined = combined.stream().sorted(comp).toList();
        }
        
        // 페이징 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combined.size());
        java.util.List<Item> pagedContent = start < combined.size() ? combined.subList(start, end) : java.util.Collections.emptyList();
        
        return new org.springframework.data.domain.PageImpl<>(
                pagedContent,
                pageable,
                combined.size()
        );
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


