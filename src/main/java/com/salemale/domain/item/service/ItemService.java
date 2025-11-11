package com.salemale.domain.item.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.item.converter.ItemConverter;
import com.salemale.domain.item.dto.request.BidRequest;
import com.salemale.domain.item.dto.request.ItemRegisterRequest;
import com.salemale.domain.item.dto.response.*;
import com.salemale.domain.item.dto.response.detail.ItemDetailResponse;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.ItemImage;
import com.salemale.domain.item.entity.ItemTransaction;
import com.salemale.domain.item.entity.UserLiked;
import com.salemale.domain.item.enums.AuctionSortType;
import com.salemale.domain.item.enums.AuctionStatus;
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.item.repository.ItemTransactionRepository;
import com.salemale.domain.item.repository.UserLikedRepository;
import com.salemale.domain.region.entity.Region;
import com.salemale.domain.s3.service.S3Service;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.repository.UserRegionRepository;
import com.salemale.domain.user.repository.UserRepository;
import com.salemale.global.common.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UserLikedRepository userLikedRepository;
    private final UserRegionRepository userRegionRepository;
    private final ItemTransactionRepository itemTransactionRepository;
    private final S3Service s3Service; // s3 로직
    private final ImageService imageService;
    private final RecommendationService recommendationService;

    //찜하기
    @Transactional
    public ItemLikeResponse likeItem(Long userId, Long itemId) {

        // 1. 사용자 조회 (UID 기반)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 상품 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ITEM_NOT_FOUND));

        // 3. 본인 상품 찜하기 방지
        if (item.getSeller().getId().equals(user.getId())) {
            throw new GeneralException(ErrorStatus.ITEM_SELF_LIKE_FORBIDDEN);
        }

        // 4. 이미 찜했는지 확인
        if (userLikedRepository.existsByUserAndItem(user, item)) {
            throw new GeneralException(ErrorStatus.ITEM_ALREADY_LIKED);
        }

        // 5. 찜하기 생성
        UserLiked userLiked = UserLiked.builder()
                .user(user)
                .item(item)
                .liked(true)
                .build();
        userLikedRepository.save(userLiked);

        // 6. DTO로 응답 반환
        return ItemLikeResponse.of(itemId, true);
    }

    /**
     * 찜 취소
     * 
     * @param userId 찜 취소하려는 사용자의 ID (JWT에서 추출)
     * @param itemId 찜 취소할 상품의 ID
     * @return 찜 취소 결과 (itemId, liked=false)
     */
    @Transactional
    public ItemLikeResponse unlikeItem(Long userId, Long itemId) {

        // 1. 사용자 조회 (UID 기반)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 상품 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ITEM_NOT_FOUND));

        // 3. 찜한 레코드 찾기
        UserLiked userLiked = userLikedRepository.findByUserAndItem(user, item)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ITEM_NOT_LIKED));

        // 4. 찜 취소 (삭제)
        userLikedRepository.delete(userLiked);

        // 5. 응답 반환
        return ItemLikeResponse.of(itemId, false);
    }

    /**
     * 이미지 업로드 (temp 폴더)
     * @param images 업로드할 이미지 파일들
     * @return 업로드된 이미지 URL 리스트
     */
    //@Transactional -> s3는 트랜잭션과 무관
    public ImageUploadResponse uploadImages(List<MultipartFile> images) {
        // 1. 이미지 개수 검증 (1~10개)
        if (images == null || images.isEmpty()) {
            throw new GeneralException(ErrorStatus.IMAGE_COUNT_INVALID);
        }
        if (images.size() > 10) {
            throw new GeneralException(ErrorStatus.IMAGE_COUNT_INVALID);
        }

        /// 2. 각 이미지를 검증하고 S3 temp 폴더에 업로드
        List<String> tempUrls = images.stream()
                .map(image -> {
                    // ImageService로 파일 검증
                    imageService.validateFile(image);
                    // S3Service로 업로드
                    return s3Service.uploadToTemp(image);
                })
                .toList();

        // 3. 응답 반환
        return ImageUploadResponse.of(tempUrls);
    }

    // 경매 상품 등록
    @Transactional
    public ItemRegisterResponse registerItem(Long sellerId, ItemRegisterRequest request) {

        // 1. 판매자 (User) 조회 (UID 기반)
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 판매자의 대표 동네 (Region) 조회 -> 테스트 위해 주석 처리
        Region region = userRegionRepository.findByPrimaryUser(seller)
                .map(userRegion -> userRegion.getRegion())
                .orElseThrow(() -> new GeneralException(ErrorStatus.REGION_NOT_SET));

        // 3. 자동 계산 값 설정 및 시간 처리, 현재 모든 상품의 최소 입찰 금액은 100원 이상 & 시작가의 5% 이상으로 설정
        Integer startPrice = request.getStartPrice();
        Integer bidIncrement = Math.max(100, (int) Math.round(startPrice * 0.05));
        LocalDateTime endTime;
        try {
            // "YYYY-MM-DDTHH:mm" 형식으로 파싱 날짜+시분 까지 입력받음
            endTime = LocalDateTime.parse(request.getEndDateTime());

            // 경매 종료 시간 검증 (현재 시간보다 미래여야 함)
            if (endTime.isBefore(LocalDateTime.now())) {
                throw new GeneralException(ErrorStatus.INVALID_END_TIME); // 또는 새로운 에러 코드
            }

        } catch (DateTimeParseException e) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST); // 잘못된 날짜/시간 형식 처리
        }

        // 상품 등록시 imageUrls 처리: tempURL(임시저장 url)을 items폴더로(영구저장 url로) 이동
        List<String> finalImageUrls = request.getImageUrls().stream()
                .map(s3Service::moveToItems)
                .toList();

        // 4. Item 엔티티 생성 및 저장
        Item newItem = Item.builder()
                .seller(seller)
                .name(request.getName())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .startPrice(startPrice)
                .currentPrice(startPrice) // 시작가 = 현재가로 초기화
                .bidIncrement(bidIncrement) // 자동 계산된 입찰 단위
                .endTime(endTime)
                .itemStatus(ItemStatus.BIDDING) // 초기 상태는 '입찰중'
                .tradeMethods(request.getTradeMethods())
                .tradeDetails(request.getTradeDetails())
                .region(region)
                .itemType(ItemType.AUCTION)  // 경매 상품 타입 추가(일반경매, 핫딜 구분)
                .build();

        // 5. ItemImage 엔티티 생성 및 연결 (순서대로 0부터 부여)
        List<ItemImage> images = IntStream.range(0, request.getImageUrls().size())
                .mapToObj(i -> ItemImage.builder()
                        .item(newItem) // Item과의 관계 설정
                        .imageUrl(finalImageUrls.get(i)) // temp url을 item url로
                        .imageOrder(i)
                        .build())
                .toList();

        // OneToMany(cascade = ALL) 설정 덕분에 Item만 저장해도 Image가 함께 저장됩니다.
        newItem.getImages().addAll(images);
        // 6. 저장 및 결과 변수 분리
        Item savedItem = itemRepository.save(newItem); // 재할당되는 변수를 분리

        // 7. Response DTO 반환
        return ItemRegisterResponse.builder()
                .sellerId(seller.getId())
                .itemId(savedItem.getItemId()) // 저장된 엔티티의 ID 사용
                .title(savedItem.getTitle())
                .startPrice(savedItem.getStartPrice())
                .endTime(savedItem.getEndTime())
                .createdAt(savedItem.getCreatedAt())
                .build();
    }

    /**
     * 경매 상품에 입찰
     * 
     * @param userId 입찰자의 사용자 ID (JWT에서 추출)
     * @param itemId 입찰할 상품의 ID
     * @param request 입찰 요청 (입찰 가격)
     * @return 입찰 결과 (거래 ID, 입찰가, 이전가, 입찰 수 등)
     */
    @Transactional
    public BidResponse bidOnItem(Long userId, Long itemId, BidRequest request) {

        // 1. 입찰자 조회 (UID 기반)
        User buyer = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 상품 조회 (비관적 락 사용 - 동시성 제어)
        Item item = itemRepository.findByIdWithLock(itemId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ITEM_NOT_FOUND));

        // 3. 입찰 전 현재가 저장 (previousPrice로 사용)
        Integer previousPrice = item.getCurrentPrice();

        // 4. 입찰 검증
        validateBid(buyer, item, request.getBidPrice());

        // 5. 입찰 거래 생성
        ItemTransaction transaction = ItemTransaction.builder()
                .buyer(buyer)
                .item(item)
                .bidPrice(request.getBidPrice())
                .build();
        ItemTransaction savedTransaction = itemTransactionRepository.save(transaction);

        // 6. Item의 현재가 업데이트
        item.updateCurrentPrice(request.getBidPrice());
        item.incrementBidCount();

        // 7. 응답 DTO 생성
        return BidResponse.builder()
                .transactionId(savedTransaction.getTransactionId())
                .itemId(item.getItemId())
                .buyerId(buyer.getId())
                .bidPrice(request.getBidPrice())
                .previousPrice(previousPrice)
                .currentHighestPrice(request.getBidPrice())
                .bidIncrement(item.getBidIncrement())
                .bidCount(item.getBidCount())
                .bidTime(savedTransaction.getCreatedAt())
                .endTime(item.getEndTime())
                .build();
    }


    //입찰 가능 여부를 검증
    private void validateBid(User buyer, Item item, Integer bidPrice) {

        // 1. 본인 상품 입찰 방지
        if (item.getSeller().getId().equals(buyer.getId())) {
            throw new GeneralException(ErrorStatus.BID_SELF_AUCTION);
        }

        // 2. 경매 상태 확인 (BIDDING 상태여야 함)
        if (!item.isBiddingStatus()) {
            throw new GeneralException(ErrorStatus.AUCTION_NOT_BIDDING);
        }

        // 3. 경매 종료 시간 확인
        if (item.isAuctionEnded()) {
            throw new GeneralException(ErrorStatus.AUCTION_ALREADY_ENDED);
        }

        // 4. 최소 입찰 가격 확인 (현재가 + 최소 입찰 단위 이상)
        long minimumBidPrice = (long) item.getCurrentPrice() + item.getBidIncrement();
        if (bidPrice < minimumBidPrice) {
            throw new GeneralException(ErrorStatus.BID_AMOUNT_TOO_LOW);
        }
    }

    /**
     * 경매 상품 상세 조회
     * @param itemId 상품 ID
     * @param email 로그인한 사용자 이메일 (nullable)
     * @param bidHistoryLimit 조회할 입찰 내역 개수
     * @return 상품 상세 정보
     */
    @Transactional //read only 제거 -> 조회수 업데이트 위해
    public ItemDetailResponse getItemDetail(Long itemId, String email, Integer bidHistoryLimit) {

        // 1. 상품 조회 (fetch join으로 연관 엔티티 함께 조회)
        Item item = itemRepository.findByIdWithDetails(itemId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ITEM_NOT_FOUND));

        // ===== 조회수 증가: 원자적 DB 업데이트 =====
        itemRepository.incrementViewCount(itemId);

        // 2. 입찰 내역 조회 (최신순, 제한된 개수)
        int limit = bidHistoryLimit != null ? bidHistoryLimit : 10;
        Pageable pageable = PageRequest.of(0, limit);
        List<ItemTransaction> bidHistory = itemTransactionRepository
                .findBidHistoryByItem(item, pageable);

        // 3. 최고 입찰 조회
        ItemTransaction highestBid = itemTransactionRepository
                .findTopByItemOrderByBidPriceDescCreatedAtAsc(item)
                .orElse(null);

        // 5. 찜 개수 조회
        Long likeCount = userLikedRepository.countByItem(item);

        // 6. 현재 사용자의 찜 여부 확인 (로그인한 경우)
        Boolean isLiked = false;
        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                isLiked = userLikedRepository.existsByUserAndItem(user, item);
            }
        }

        // 7. Converter를 통해 DTO 변환
        return ItemConverter.toItemDetailResponse(
                item, bidHistory, highestBid, likeCount, isLiked
        );
    }

    /**
     * 경매 상품 리스트 조회
     *
     * @param status 상태 필터 (BIDDING, COMPLETED, POPULAR)
     * @param categories 카테고리 필터(다중선택 가능)
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @param sortType 정렬 타입
     * @param pageable 페이징 정보
     * @return 경매 상품 리스트와 페이징 정보
     */
    @Transactional(readOnly = true)
    public AuctionListResponse getAuctionList(
            AuctionStatus status,
            List<Category> categories,
            Integer minPrice,
            Integer maxPrice,
            AuctionSortType sortType,
            Pageable pageable
    ) {
        // 1. QueryDSL로 동적 쿼리 실행 (이미 DB에서 정렬됨)
        Page<Item> itemPage = itemRepository.findAuctionList(
                status, categories, minPrice, maxPrice, sortType, pageable
        );

        // 2. DTO 변환 (엔티티의 bidCount 사용)
        List<AuctionListItemDTO> items = itemPage.getContent().stream()
                .map(item -> ItemConverter.toAuctionListItemDTO(item))
                .toList();

        // 3. 페이징 정보와 함께 응답 DTO 생성
        return AuctionListResponse.builder()
                .items(items)
                .totalElements(itemPage.getTotalElements())
                .totalPages(itemPage.getTotalPages())
                .currentPage(itemPage.getNumber())
                .size(itemPage.getSize())
                .hasNext(itemPage.hasNext())
                .hasPrevious(itemPage.hasPrevious())
                .build();
    }

    /**
     * 개인화 추천 경매 상품 리스트 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 추천 경매 상품 리스트 (페이징 포함)
     */
    @Transactional(readOnly = true)
    public AuctionListResponse getRecommendedAuctionList(Long userId, Pageable pageable) {
        log.info("[추천 상품 조회] 사용자 ID: {}, 페이지: {}", userId, pageable.getPageNumber());

        // 1. 파이썬 추천 API 호출
        List<Long> recommendedItemIds = recommendationService.getRecommendedItemIds(userId);

        // 2. 추천 결과가 없으면 인기 상품으로 대체
        if (recommendedItemIds.isEmpty()) {
            log.info("[추천 대체] 사용자 ID: {}, 인기 상품으로 대체", userId);
            return getAuctionList(AuctionStatus.POPULAR, null, null, null,
                    AuctionSortType.BID_COUNT_DESC, pageable);
        }

        // 3. 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), recommendedItemIds.size());

        // 4. 페이지 범위를 벗어나면 빈 결과 반환
        if (start >= recommendedItemIds.size()) {
            return AuctionListResponse.builder()
                    .items(Collections.emptyList())
                    .totalElements((long) recommendedItemIds.size())
                    .totalPages((int) Math.ceil((double) recommendedItemIds.size() / pageable.getPageSize()))
                    .currentPage(pageable.getPageNumber())
                    .size(0)
                    .hasNext(false)
                    .hasPrevious(start > 0)
                    .build();
        }

        List<Long> pagedItemIds = recommendedItemIds.subList(start, end);

        // 5. 실제 상품 정보 조회
        List<AuctionListItemDTO> items = recommendationService.getRecommendedItems(pagedItemIds);

        // 6. 페이징 정보와 함께 응답 반환
        int totalElements = recommendedItemIds.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        log.info("[추천 상품 조회 완료] 사용자 ID: {}, 총 추천: {}개, 반환: {}개",
                userId, totalElements, items.size());

        return AuctionListResponse.builder()
                .items(items)
                .totalElements((long) totalElements)
                .totalPages(totalPages)
                .currentPage(pageable.getPageNumber())
                .size(items.size())
                .hasNext(end < recommendedItemIds.size())
                .hasPrevious(start > 0)
                .build();
    }
}