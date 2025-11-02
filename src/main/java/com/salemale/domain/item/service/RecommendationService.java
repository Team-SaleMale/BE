package com.salemale.domain.item.service;

import com.salemale.domain.item.converter.ItemConverter;
import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final WebClient webClient;
    private final ItemRepository itemRepository;

    @Value("${recommendation.api.url:http://localhost:8000}")
    private String recommendationApiUrl;

    /**
     * 파이썬 추천 API 응답 DTO (내부 클래스)
     */
    private static class RecommendationApiResponse {
        private List<RecommendedItem> recommended_items;

        public List<RecommendedItem> getRecommended_items() {
            return recommended_items != null ? recommended_items : Collections.emptyList();
        }

        public void setRecommended_items(List<RecommendedItem> recommended_items) {
            this.recommended_items = recommended_items;
        }

        private static class RecommendedItem {
            private Long item_id;

            public Long getItem_id() {
                return item_id;
            }

            public void setItem_id(Long item_id) {
                this.item_id = item_id;
            }
        }
    }

    /**
     * 파이썬 추천 API를 호출하여 추천 상품 ID 리스트를 가져옵니다.
     *
     * @param userId 사용자 ID
     * @return 추천 상품 ID 리스트
     */
    public List<Long> getRecommendedItemIds(Long userId) {
        try {
            log.info("[추천 API 호출] 사용자 ID: {}, URL: {}", userId, recommendationApiUrl);

            RecommendationApiResponse response = webClient
                    .post()
                    .uri(recommendationApiUrl + "/recommend-auctions")
                    .bodyValue(Map.of("user_id", userId))
                    .retrieve()
                    .bodyToMono(RecommendationApiResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .onErrorResume(e -> {
                        log.error("[추천 API 오류] 사용자 ID: {}, 오류: {}", userId, e.getMessage());
                        return Mono.just(new RecommendationApiResponse());
                    })
                    .block();

            if (response == null || response.getRecommended_items().isEmpty()) {
                log.warn("[추천 API 빈 응답] 사용자 ID: {}", userId);
                return Collections.emptyList();
            }

            List<Long> itemIds = response.getRecommended_items().stream()
                    .map(RecommendationApiResponse.RecommendedItem::getItem_id)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("[추천 API 성공] 사용자 ID: {}, 추천 상품 수: {}", userId, itemIds.size());
            return itemIds;

        } catch (Exception e) {
            log.error("[추천 API 예외] 사용자 ID: {}, 예외: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 추천 상품 ID 리스트를 기반으로 실제 상품 정보를 조회합니다.
     *
     * @param itemIds 추천 상품 ID 리스트
     * @return 추천 상품 DTO 리스트 (추천 순서 유지)
     */
    public List<AuctionListItemDTO> getRecommendedItems(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }

        // DB에서 상품 정보 조회 (이미지 fetch join)
        // LEFT JOIN FETCH로 인해 이미지가 여러 개인 경우 중복된 Item이 반환될 수 있음
        List<Item> items = itemRepository.findAllByItemIdInWithImages(itemIds);

        // 중복 제거: toMap을 사용하여 itemId를 키로 하는 Map 생성 (중복 시 마지막 값 유지)
        // itemIds 순서대로 정렬하기 위해 Map 사용
        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(
                        Item::getItemId, 
                        item -> item,
                        (existing, replacement) -> existing,  // 중복 시 기존 값 유지
                        LinkedHashMap::new  // 순서 보장
                ));

        // 추천 순서 유지하면서 DTO 변환
        return itemIds.stream()
                .map(itemMap::get)
                .filter(Objects::nonNull)  // null 제거 (DB에 없는 상품)
                .map(ItemConverter::toAuctionListItemDTO)
                .collect(Collectors.toList());
    }
}