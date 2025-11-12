package com.salemale.domain.item.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.QItemTransaction;
import com.salemale.domain.item.enums.AuctionSortType;
import com.salemale.domain.item.enums.AuctionStatus;
import com.salemale.domain.mypage.enums.MyAuctionSortType;
import com.salemale.domain.mypage.enums.MyAuctionType;
import com.salemale.domain.user.entity.User;
import com.salemale.global.common.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.salemale.domain.item.entity.QItem.item;

/**
 * Item Repository QueryDSL 구현체
 */
@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Item> findAuctionList(
            AuctionStatus status,
            List<Category> categories,  // ← 변경
            Integer minPrice,
            Integer maxPrice,
            AuctionSortType sortType,
            Pageable pageable
    ) {
        // 시간을 딱 한 번만 계산해서 변수에 저장(데이터 불일치 방지)
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime threeDaysAgo = now.minusDays(3);

        // Step 1: ID만 페이징해서 조회 (fetch join 없음)
        List<Long> itemIds = queryFactory
                .select(item.itemId)
                .from(item)
                .where(
                        itemTypeIsAuction(),  // 추가: 일반 경매만 조회하도록
                        statusCondition(status, now, threeDaysAgo),
                        categoryCondition(categories),
                        priceRangeCondition(minPrice, maxPrice)
                )
                .orderBy(getOrderSpecifier(sortType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Step 2: 조회된 ID로 전체 데이터 + 이미지 fetch join
        List<Item> content = Collections.emptyList();
        if (!itemIds.isEmpty()) {
            content = queryFactory
                    .selectFrom(item)
                    .leftJoin(item.images).fetchJoin()
                    .where(item.itemId.in(itemIds))
                    .orderBy(getOrderSpecifier(sortType))  // 같은 정렬 유지
                    .fetch();
        }

        // Count 쿼리 (성능 최적화)
        JPAQuery<Long> countQuery = queryFactory
                .select(item.count())
                .from(item)
                .where(
                        itemTypeIsAuction(),  // 추가: 일반 경매만
                        statusCondition(status, now, threeDaysAgo),
                        categoryCondition(categories),  // ← 변경
                        priceRangeCondition(minPrice, maxPrice)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 상태별 필터 조건
     */
    private BooleanExpression statusCondition(AuctionStatus status, LocalDateTime now, LocalDateTime threeDaysAgo) {
        if (status == null) {
            return null;
        }

        // 이제 내부에서 시간을 계산하지 않음
        return switch (status) {
            case BIDDING -> item.itemStatus.eq(ItemStatus.BIDDING)
                    .and(item.endTime.after(now));

            case COMPLETED -> item.itemStatus.in(ItemStatus.SUCCESS, ItemStatus.FAIL)
                    .or(item.endTime.before(now));

            case POPULAR -> item.itemStatus.eq(ItemStatus.BIDDING)
                    .and(item.endTime.after(now))
                    .and(item.createdAt.after(threeDaysAgo))
                    .and(item.bidCount.goe(3L));

            default -> null;
        };
    }

    /**
     * 카테고리 필터 조건 (다중 선택)
     */
    private BooleanExpression categoryCondition(List<Category> categories) {
        // categories가 null이거나 비어있으면 필터 없음
        return (categories != null && !categories.isEmpty())
                ? item.category.in(categories)
                : null;
    }

    /**
     * 가격 범위 필터 조건
     * 규칙:
     * - min:0, max:0 → 가격 조건 없음 (전체 조회)
     * - min:100, max:0 → 잘못된 조건 (결과 없음)
     * - min:0, max:500 → 최대 500까지
     * - min:100, max:500 → 100~500 사이
     */
    private BooleanExpression priceRangeCondition(Integer minPrice, Integer maxPrice) {
        // 둘 다 0이면 가격 필터 사용 안 함
        if ((minPrice == null || minPrice == 0) && (maxPrice == null || maxPrice == 0)) {
            return null;
        }

        // 둘 다 값이 있는 경우
        if (minPrice != null && minPrice > 0 && maxPrice != null && maxPrice > 0) {
            return item.currentPrice.between(minPrice, maxPrice);
        }

        // minPrice만 있는 경우 (max가 0 또는 null)
        if (minPrice != null && minPrice > 0) {
            if (maxPrice != null && maxPrice == 0) {
                // min:100, max:0 같은 잘못된 조건 → 결과 없음
                return Expressions.FALSE.isTrue();  // 항상 false가 되는 조건
            }
            return item.currentPrice.goe(minPrice);
        }

        // maxPrice만 있는 경우 (min이 0 또는 null)
        if (maxPrice != null && maxPrice > 0) {
            return item.currentPrice.loe(maxPrice);
        }

        return null;
    }

    /**
     * 정렬 조건, 보조키로 itemId를 추가해서 페이징시 상품 일관성을 유지함
     */
    private OrderSpecifier<?>[] getOrderSpecifier(AuctionSortType sortType) {
        if (sortType == null) {
            return new OrderSpecifier<?>[]{
                    item.createdAt.desc(),
                    item.itemId.asc()
            };
        }

        return switch (sortType) {
            case CREATED_DESC -> new OrderSpecifier<?>[]{item.createdAt.desc(), item.itemId.asc()};
            case PRICE_ASC -> new OrderSpecifier<?>[]{item.currentPrice.asc(), item.itemId.asc()};
            case PRICE_DESC -> new OrderSpecifier<?>[]{item.currentPrice.desc(), item.itemId.asc()};
            case VIEW_COUNT_DESC -> new OrderSpecifier<?>[]{item.viewCount.desc(), item.itemId.asc()};
            case END_TIME_ASC -> new OrderSpecifier<?>[]{item.endTime.asc(), item.itemId.asc()};
            case BID_COUNT_DESC -> new OrderSpecifier<?>[]{item.bidCount.desc(), item.itemId.asc()};
        };
    }

    /**
     * 내 경매 목록 조회
     *
     * @param user 사용자
     * @param type 경매 타입 (ALL, SELLING, BIDDING, WON, FAILED)
     * @param sortType 정렬 타입 (CREATED_DESC, PRICE_DESC, PRICE_ASC)
     * @param pageable 페이징 정보
     * @return 내 경매 목록 (페이징)
     */
    @Override
    public Page<Item> findMyAuctions(
            User user,
            MyAuctionType type,
            MyAuctionSortType sortType,
            Pageable pageable
    ) {
        // Phase 1: ID만 조회 (fetch join 없이)
        List<Long> itemIds = queryFactory
                .select(item.itemId)
                .from(item)
                .where(myAuctionCondition(user, type))
                .orderBy(getMyAuctionOrderSpecifier(sortType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Phase 2: 전체 데이터 + 이미지 fetch join
        List<Item> content = Collections.emptyList();
        if (!itemIds.isEmpty()) {
            content = queryFactory
                    .selectFrom(item)
                    .leftJoin(item.images).fetchJoin()
                    .where(item.itemId.in(itemIds))
                    .orderBy(getMyAuctionOrderSpecifier(sortType))
                    .fetch();
        }

        // Count 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(item.count())
                .from(item)
                .where(myAuctionCondition(user, type));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 내 경매 타입별 필터 조건
     */
    private BooleanExpression myAuctionCondition(User user, MyAuctionType type) {
        if (type == null) {
            type = MyAuctionType.ALL;
        }

        return switch (type) {
            case ALL -> myAllAuctions(user);
            case SELLING -> mySelling(user);
            case BIDDING -> myBidding(user);
            case WON -> myWon(user);
            case FAILED -> myFailed(user);
        };
    }

    /**
     * 전체: 내가 판매하거나 입찰한 모든 상품
     */
    private BooleanExpression myAllAuctions(User user) {
        QItemTransaction transaction = QItemTransaction.itemTransaction;

        // 내가 판매자이거나, 입찰한 상품
        return item.seller.eq(user)
                .or(JPAExpressions
                        .selectOne()
                        .from(transaction)
                        .where(
                                transaction.item.eq(item),
                                transaction.buyer.eq(user)
                        )
                        .exists()
                );
    }

    /**
     * 판매: 내가 판매자인 상품
     */
    private BooleanExpression mySelling(User user) {
        return item.seller.eq(user);
    }

    /**
     * 입찰: 내가 입찰한 상품 (판매자가 아닌 상품만)
     */
    private BooleanExpression myBidding(User user) {
        QItemTransaction transaction = QItemTransaction.itemTransaction;

        // 내가 입찰했고, 판매자가 아닌 상품
        return JPAExpressions
                .selectOne()
                .from(transaction)
                .where(
                        transaction.item.eq(item),
                        transaction.buyer.eq(user)
                )
                .exists()
                .and(item.seller.ne(user));  // 판매자는 제외
    }

    /**
     * 낙찰: 경매가 종료되고 내가 낙찰자로 확정된 상품
     * - winner가 나인 경우만 (경매 종료 후 확정)
     */
    private BooleanExpression myWon(User user) {
        // 경매 마감 후 winner가 결정되므로 SUCCESS 상태 확인
        return item.winner.eq(user)
                .and(item.itemStatus.eq(ItemStatus.SUCCESS));
    }

    /**
     * 유찰: 내가 판매자이고 상태가 FAIL인 상품
     */
    private BooleanExpression myFailed(User user) {
        return item.seller.eq(user)
                .and(item.itemStatus.eq(ItemStatus.FAIL));
    }

    /**
     * 내 경매 정렬 조건
     * @param sortType 정렬 타입
     * @return 정렬 조건 배열 (주 정렬 + itemId 보조 정렬)
     */
    private OrderSpecifier<?>[] getMyAuctionOrderSpecifier(MyAuctionSortType sortType) {
        if (sortType == null) {
            sortType = MyAuctionSortType.CREATED_DESC;
        }

        return switch (sortType) {
            case CREATED_DESC -> new OrderSpecifier<?>[]{
                    item.createdAt.desc(),
                    item.itemId.asc()
            };
            case PRICE_DESC -> new OrderSpecifier<?>[]{
                    item.currentPrice.desc(),
                    item.itemId.asc()
            };
            case PRICE_ASC -> new OrderSpecifier<?>[]{
                    item.currentPrice.asc(),
                    item.itemId.asc()
            };
        };
    }

    // 추가: 핫딜 상품 리스트 조회
    @Override
    public Page<Item> findHotdealList(
            Integer minPrice,
            Integer maxPrice,
            AuctionSortType sortType,
            Pageable pageable
    ) {
        // Step 1: ID만 페이징해서 조회
        List<Long> itemIds = queryFactory
                .select(item.itemId)
                .from(item)
                .where(
                        itemTypeIsHotdeal(),  // 핫딜만
                        item.itemStatus.eq(ItemStatus.BIDDING),  // 입찰 중만
                        priceRangeCondition(minPrice, maxPrice)
                )
                .orderBy(getOrderSpecifier(sortType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Step 2: 조회된 ID로 전체 데이터 + 이미지 fetch join
        List<Item> content = Collections.emptyList();
        if (!itemIds.isEmpty()) {
            content = queryFactory
                    .selectFrom(item)
                    .leftJoin(item.images).fetchJoin()
                    .leftJoin(item.hotdealStore).fetchJoin()  // 핫딜 가게 정보
                    .leftJoin(item.region).fetchJoin()  // region 정보
                    .where(item.itemId.in(itemIds))
                    .orderBy(getOrderSpecifier(sortType))
                    .fetch();
        }

        // Count 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(item.count())
                .from(item)
                .where(
                        itemTypeIsHotdeal(),
                        item.itemStatus.eq(ItemStatus.BIDDING),
                        priceRangeCondition(minPrice, maxPrice)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // 🔥 추가: itemType 조건 메서드들
    private BooleanExpression itemTypeIsAuction() {
        return item.itemType.eq(ItemType.AUCTION);
    }

    private BooleanExpression itemTypeIsHotdeal() {
        return item.itemType.eq(ItemType.HOTDEAL);
    }
}