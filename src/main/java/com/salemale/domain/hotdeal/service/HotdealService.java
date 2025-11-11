package com.salemale.domain.hotdeal.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.hotdeal.dto.request.HotdealRegisterRequest;
import com.salemale.domain.hotdeal.dto.response.HotdealListItemDTO;
import com.salemale.domain.hotdeal.dto.response.HotdealListResponse;
import com.salemale.domain.hotdeal.entity.HotdealStore;
import com.salemale.domain.item.converter.ItemConverter;
import com.salemale.domain.item.dto.response.AuctionListItemDTO;
import com.salemale.domain.item.dto.response.AuctionListResponse;
import com.salemale.domain.item.dto.response.ItemRegisterResponse;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.ItemImage;
import com.salemale.domain.item.enums.AuctionSortType;
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.s3.service.S3Service;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.repository.UserRepository;
import com.salemale.global.common.enums.Category;
import com.salemale.global.common.enums.ItemStatus;
import com.salemale.global.common.enums.ItemType;
import com.salemale.global.common.enums.TradeMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 핫딜 상품 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotdealService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final HotdealStoreService hotdealStoreService;
    private final S3Service s3Service;

    /**
     * 핫딜 상품 등록
     * @param userId 핫딜 판매자 ID
     * @param request 핫딜 상품 등록 요청
     * @return 등록된 상품 정보
     */
    @Transactional
    public ItemRegisterResponse registerHotdeal(Long userId, HotdealRegisterRequest request) {
        log.info("[핫딜 상품 등록] 사용자 ID: {}, 상품명: {}", userId, request.getName());

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 핫딜 가게 정보 조회 (권한 검증 포함)
        HotdealStore store = hotdealStoreService.validateAndGetStore(userId);

        // 3. 자동 계산 값 설정
        Integer startPrice = request.getStartPrice();
        Integer bidIncrement = Math.max(100, (int) Math.round(startPrice * 0.05));

        // 4. 경매 종료 시간 파싱 및 검증
        LocalDateTime endTime;
        try {
            endTime = LocalDateTime.parse(request.getEndDateTime());

            // 현재 시간보다 미래여야 함
            if (endTime.isBefore(LocalDateTime.now())) {
                throw new GeneralException(ErrorStatus.INVALID_END_TIME);
            }
        } catch (DateTimeParseException e) {
            log.error("[날짜 파싱 실패] endDateTime: {}", request.getEndDateTime());
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        // 5. 이미지 처리 (temp -> items 폴더로 이동)
        List<String> finalImageUrls = request.getImageUrls().stream()
                .map(s3Service::moveToItems)
                .toList();

        // 6. 핫딜 Item 엔티티 생성 (디폴트값 자동 설정)
        Item hotdealItem = Item.builder()
                .seller(user)
                .name(request.getName())
                .title(store.getStoreName())  // 가게명으로 자동 설정
                .description(request.getDescription())
                .category(Category.ETC)  // 디폴트: 기타
                .startPrice(startPrice)
                .currentPrice(startPrice)
                .bidIncrement(bidIncrement)
                .endTime(endTime)
                .itemStatus(ItemStatus.BIDDING)
                .tradeMethods(List.of(TradeMethod.IN_PERSON))  // 디폴트: 직거래
                .tradeDetails(store.getAddress())  // 가게 주소로 자동 설정
                .region(store.getRegion())  // 가게의 지역 사용
                .itemType(ItemType.HOTDEAL)  // 핫딜 타입
                .hotdealStore(store)  // 가게 정보 연결
                .build();

        // 7. ItemImage 엔티티 생성 및 연결
        List<ItemImage> images = IntStream.range(0, finalImageUrls.size())
                .mapToObj(i -> ItemImage.builder()
                        .item(hotdealItem)
                        .imageUrl(finalImageUrls.get(i))
                        .imageOrder(i)
                        .build())
                .toList();

        hotdealItem.getImages().addAll(images);

        // 8. 저장
        Item savedItem = itemRepository.save(hotdealItem);

        log.info("[핫딜 상품 등록 완료] 상품 ID: {}, 가게명: {}", savedItem.getItemId(), store.getStoreName());

        // 9. 응답 DTO 반환
        return ItemRegisterResponse.builder()
                .sellerId(user.getId())
                .itemId(savedItem.getItemId())
                .title(savedItem.getTitle())
                .startPrice(savedItem.getStartPrice())
                .endTime(savedItem.getEndTime())
                .createdAt(savedItem.getCreatedAt())
                .build();
    }

    /**
     * 핫딜 상품 리스트 조회
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @param sortType 정렬 타입
     * @param pageable 페이징 정보
     * @return 핫딜 상품 리스트 (페이징)
     */
    /**
     * 핫딜 상품 리스트 조회
     */
    public HotdealListResponse getHotdealList(  // 🔥 반환 타입 변경
                                                Integer minPrice,
                                                Integer maxPrice,
                                                AuctionSortType sortType,
                                                Pageable pageable
    ) {
        log.info("[핫딜 리스트 조회] minPrice: {}, maxPrice: {}, sortType: {}, page: {}",
                minPrice, maxPrice, sortType, pageable.getPageNumber());

        // 1. QueryDSL로 핫딜 상품 조회
        Page<Item> itemPage = itemRepository.findHotdealList(
                minPrice, maxPrice, sortType, pageable
        );

        // 2. DTO 변환 (핫딜 전용 DTO 사용)
        List<HotdealListItemDTO> items = itemPage.getContent().stream()
                .map(ItemConverter::toHotdealListItemDTO)  // 🔥 변경
                .toList();

        log.info("[핫딜 리스트 조회 완료] 총 {}개, 현재 페이지 {}개",
                itemPage.getTotalElements(), items.size());

        // 3. 페이징 정보와 함께 응답 DTO 생성
        return HotdealListResponse.builder()  // 🔥 변경
                .items(items)
                .totalElements(itemPage.getTotalElements())
                .totalPages(itemPage.getTotalPages())
                .currentPage(itemPage.getNumber())
                .size(itemPage.getSize())
                .hasNext(itemPage.hasNext())
                .hasPrevious(itemPage.hasPrevious())
                .build();
    }
}