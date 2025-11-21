package com.salemale.domain.mypage.service;

import com.salemale.common.code.status.ErrorStatus;
import com.salemale.common.exception.GeneralException;
import com.salemale.domain.item.converter.ItemConverter;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.ItemTransaction;
import com.salemale.domain.item.entity.Review;
import com.salemale.domain.item.repository.ItemRepository;
import com.salemale.domain.item.repository.ItemTransactionRepository;
import com.salemale.domain.item.repository.ReviewRepository;
import com.salemale.domain.item.repository.UserLikedRepository;
import com.salemale.domain.mypage.dto.response.*;
import com.salemale.domain.user.entity.User;
import com.salemale.domain.user.entity.UserPreferredCategory;
import com.salemale.domain.user.repository.UserPreferredCategoryRepository;
import com.salemale.domain.user.repository.UserRepository;
import com.salemale.domain.mypage.enums.MyRole;
import com.salemale.domain.mypage.enums.MyAuctionSortType;
import com.salemale.domain.mypage.enums.MyAuctionType;
import com.salemale.global.common.enums.Category;
import com.salemale.global.common.enums.ItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemTransactionRepository itemTransactionRepository;
    private final UserLikedRepository userLikedRepository;
    private final UserPreferredCategoryRepository preferredCategoryRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 내 경매 목록 조회
     */
    public MyAuctionListResponse getMyAuctions(
            Long userId,
            MyAuctionType type,
            MyAuctionSortType sortType,
            Pageable pageable
    ) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 내 경매 목록 조회
        Page<Item> itemPage = itemRepository.findMyAuctions(user, type, sortType, pageable);

        // 3. N+1 방지: 모든 상품의 최고가 입찰을 한 번에 조회
        List<Long> itemIds = itemPage.getContent().stream()
                .map(Item::getItemId)
                .toList();

        // 4. 최고가 입찰 목록을 Map으로 변환 (itemId -> ItemTransaction)
        Map<Long, ItemTransaction> highestBidMap = itemTransactionRepository
                .findHighestBidsByItemIds(itemIds)
                .stream()
                .collect(Collectors.toMap(
                        tx -> tx.getItem().getItemId(),
                        tx -> tx
                ));

        // 5. DTO 변환 - Service에서 비즈니스 로직으로 판단
        List<MyAuctionItemDTO> items = itemPage.getContent().stream()
                .map(item -> {
                    // 비즈니스 로직: 역할 판단
                    MyRole myRole = determineMyRole(item, user);
                    // 비즈니스 로직: 최고가 입찰자 여부 판단
                    Boolean isHighestBidder = isUserHighestBidder(item, user, highestBidMap);
                    return ItemConverter.toMyAuctionItemDTO(item, myRole, isHighestBidder);
                })
                .toList();

        // 6. Summary 정보 조회
        MyAuctionSummaryDTO summary = getMyAuctionSummary(user);

        // 7. 응답 생성
        return MyAuctionListResponse.builder()
                .items(items)
                .summary(summary)
                .totalElements(itemPage.getTotalElements())
                .totalPages(itemPage.getTotalPages())
                .currentPage(itemPage.getNumber())
                .size(itemPage.getSize())
                .hasNext(itemPage.hasNext())
                .hasPrevious(itemPage.hasPrevious())
                .build();
    }

    /**
     * 비즈니스 로직: 현재 사용자의 상품에 대한 역할 판단
     *
     * @param item 상품
     * @param currentUser 현재 사용자
     * @return SELLER, WINNER, BIDDER
     */
    public MyRole determineMyRole(Item item, User currentUser) {
        // 1. 낙찰자인지 확인
        if (item.getWinner() != null &&
                item.getWinner().getId().equals(currentUser.getId())) {
            return MyRole.WINNER;
        }

        // 2. 판매자인지 확인
        if (item.getSeller() != null &&
                item.getSeller().getId().equals(currentUser.getId())) {
            return MyRole.SELLER;
        }

        // 3. 그 외는 입찰자
        return MyRole.BIDDER;
    }

    /**
     * 비즈니스 로직: 사용자가 해당 상품의 최고가 입찰자인지 판단
     * @param item 상품
     * @param user 사용자
     * @param highestBidMap 최고가 입찰 맵 (itemId -> ItemTransaction)
     * @return 최고가 입찰자 여부
     */
    private Boolean isUserHighestBidder(
            Item item,
            User user,
            Map<Long, ItemTransaction> highestBidMap
    ) {
        ItemTransaction highestBid = highestBidMap.get(item.getItemId());

        if (highestBid == null) {
            return false;  // 입찰이 없는 경우
        }

        // 최고가 입찰자의 ID와 현재 사용자 ID 비교
        return highestBid.getBuyer().getId().equals(user.getId());
    }

    /**
     * 내 경매 요약 정보 조회
     * @param user 사용자
     * @return 요약 정보 (전체, 판매, 입찰, 낙찰, 유찰 개수)
     */
    private MyAuctionSummaryDTO getMyAuctionSummary(User user) {
        // 판매 중 개수
        Long sellingCount = itemRepository.countBySeller(user);

        // 입찰한 개수 (중복 제거)
        Long biddingCount = itemTransactionRepository.countDistinctItemByBuyer(user);

        // 낙찰받은 개수
        Long wonCount = itemRepository.countByWinner(user);

        // 유찰된 개수
        Long failedCount = itemRepository.countBySellerAndItemStatus(user, ItemStatus.FAIL);

        // 전체 개수 (판매 + 입찰, 중복은 DB 쿼리에서 처리됨)
        Long totalCount = sellingCount + biddingCount;

        return MyAuctionSummaryDTO.builder()
                .totalCount(totalCount)
                .sellingCount(sellingCount)
                .biddingCount(biddingCount)
                .wonCount(wonCount)
                .failedCount(failedCount)
                .build();
    }

    /**
     * 찜한 상품 목록 조회 (페이징)
     * - 최신 찜한 순으로 고정 정렬
     * @param userId 사용자 ID (JWT에서 추출)
     * @param pageable 페이징 정보 (page, size)
     * @return 찜한 상품 목록과 페이징 정보
     */
    public LikedItemListResponse getLikedItems(
            Long userId,
            Pageable pageable
    ) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 찜한 상품 목록 조회 (페이징, 최신순 정렬)
        Page<com.salemale.domain.item.entity.UserLiked> likedPage =
                userLikedRepository.findLikedItemsByUser(user, pageable);

        // 3. DTO 변환
        List<com.salemale.domain.mypage.dto.response.LikedItemDTO> likedItems =
                likedPage.getContent().stream()
                        .map(ItemConverter::toLikedItemDTO)
                        .toList();

        // 4. 페이징 정보와 함께 응답 DTO 생성
        return LikedItemListResponse.builder()
                .likedItems(likedItems)
                .totalElements(likedPage.getTotalElements())
                .totalPages(likedPage.getTotalPages())
                .currentPage(likedPage.getNumber())
                .size(likedPage.getSize())
                .hasNext(likedPage.hasNext())
                .hasPrevious(likedPage.hasPrevious())
                .build();
    }

    /**
     * 선호 카테고리 설정
     */
    @Transactional
    public PreferredCategoryResponse updatePreferredCategories(
            Long userId,
            List<Category> categories
    ) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 기존 선호 카테고리 전체 삭제, 바로 db에 저장(flush)
        preferredCategoryRepository.deleteByUser(user);

        preferredCategoryRepository.flush();

        // 3. 새로운 선호 카테고리 저장
        List<UserPreferredCategory> newPreferences = categories.stream()
                .distinct() // 중복 제거
                .map(category -> UserPreferredCategory.builder()
                        .user(user)
                        .category(category)
                        .build())
                .toList();

        preferredCategoryRepository.saveAll(newPreferences);

        // 4. 응답 생성
        return PreferredCategoryResponse.builder()
                .categories(categories.stream().distinct().toList())
                .count(categories.size())
                .build();
    }

    /**
     * 선호 카테고리 조회
     */
    public PreferredCategoryResponse getPreferredCategories(Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 선호 카테고리 조회
        List<Category> categories = preferredCategoryRepository.findByUser(user)
                .stream()
                .map(UserPreferredCategory::getCategory)
                .toList();

        // 3. 응답 생성
        return PreferredCategoryResponse.builder()
                .categories(categories)
                .count(categories.size())
                .build();
    }

    /**
     * 내가 받은 후기 목록 조회
     *
     * @param userId 로그인한 사용자 ID
     * @param pageable 페이징 정보
     * @return 받은 후기 목록 (페이징)
     */
    @Transactional(readOnly = true)
    public ReceivedReviewsResponse getReceivedReviews(Long userId, Pageable pageable) {

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. 후기 목록 조회 (페이징, 최신순)
        Page<Review> reviewPage = reviewRepository.findByTargetOrderByCreatedAtDesc(user, pageable);

        // 3. Review 엔티티를 DTO로 변환
        List<ReceivedReviewDTO> reviewDTOs = reviewPage.getContent().stream()
                .map(ReceivedReviewDTO::from)
                .toList();

        // 4. 응답 DTO 생성
        return ReceivedReviewsResponse.builder()
                .reviews(reviewDTOs)
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .currentPage(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .hasNext(reviewPage.hasNext())
                .hasPrevious(reviewPage.hasPrevious())
                .build();
    }
}