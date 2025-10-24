package com.salemale.domain.item.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.salemale.domain.item.entity.Item;
import com.salemale.global.common.enums.AuctionSortType;
import com.salemale.global.common.enums.AuctionStatus;
import com.salemale.global.common.enums.Category;
import com.salemale.global.common.enums.ItemStatus;
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
                .distinct()  // 중복 제거
                .where(
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
                    .leftJoin(item.images).fetchJoin()  // 이제는 안전함!
                    .where(item.itemId.in(itemIds))
                    .orderBy(getOrderSpecifier(sortType))  // 같은 정렬 유지
                    .fetch();
        }

        // Count 쿼리 (성능 최적화)
        JPAQuery<Long> countQuery = queryFactory
                .select(item.count())
                .from(item)
                .where(
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
     * 정렬 조건
     */
    private OrderSpecifier<?> getOrderSpecifier(AuctionSortType sortType) {
        if (sortType == null) {
            return item.createdAt.desc();
        }

        return switch (sortType) {
            case CREATED_DESC -> item.createdAt.desc();
            case PRICE_ASC -> item.currentPrice.asc();
            case PRICE_DESC -> item.currentPrice.desc();
            case VIEW_COUNT_DESC -> item.viewCount.desc();
            case END_TIME_ASC -> item.endTime.asc();
            case BID_COUNT_DESC -> item.bidCount.desc();
        };
    }
}