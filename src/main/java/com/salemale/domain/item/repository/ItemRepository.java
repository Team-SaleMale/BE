package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.user.entity.User;
import com.salemale.global.common.enums.ItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom{
    // 종료 시간이 지났고 아직 상태가 bidding(입찰중)인 상품들 조회
    List<Item> findByEndTimeBeforeAndItemStatus(
            LocalDateTime currentTime,
            ItemStatus status
    );

    // 우선은 비관적 락을 사용하여 상품을 조회하도록 구현. 추후 리팩토링 시 메시지큐 적용 예정
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.itemId = :itemId")
    Optional<Item> findByIdWithLock(@Param("itemId") Long itemId);

    //상품 상세 조회 시 필요한 연관 엔티티를 한 번에 조회 (N+1 방지)
    @Query("SELECT i FROM Item i " +
            "JOIN FETCH i.seller " +
            "JOIN FETCH i.region " +
            "LEFT JOIN FETCH i.images " +
            "WHERE i.itemId = :itemId")
    Optional<Item> findByIdWithDetails(@Param("itemId") Long itemId);

    /**
     * 조회수를 원자적으로 1 증가시킵니다.
     * DB 레벨에서 UPDATE 쿼리를 실행하여 동시성 문제를 방지합니다.
     *
     * @param itemId 상품 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Item i SET i.viewCount = i.viewCount + 1 WHERE i.itemId = :itemId")
    int incrementViewCount(@Param("itemId") Long itemId);

    // 내가 판매자인 모든 상품 개수 (모든 상태 포함)

    /**
     * 중심 좌표(lat, lon)로부터 distanceKm 이내의 아이템을 조회합니다(하버사인).
     * - Postgres 기준 native SQL 사용, Region의 latitude/longitude는 numeric이라 double로 캐스팅합니다.
     */
    @Query(value = """
            SELECT i.*
            FROM item i
            JOIN region r ON i.region_id = r.region_id
            WHERE i.item_type = 'AUCTION'
              AND i.item_status = CAST(:status AS varchar)
              AND (
                   :me IS NULL OR i.seller_id NOT IN (
                       SELECT bl.blocked_user_id
                       FROM block_list bl
                       WHERE bl.blocker_id = :me
                   )
               )
              AND (
                6371 * acos(
                  LEAST(1, GREATEST(-1,
                    cos(radians(:lat)) * cos(radians(CAST(r.latitude AS double precision))) *
                    cos(radians(CAST(r.longitude AS double precision)) - radians(:lon)) +
                    sin(radians(:lat)) * sin(radians(CAST(r.latitude AS double precision)))
                  ))
                )
              ) <= :distanceKm
            ORDER BY i.created_at DESC
            """,
            countQuery = """
            SELECT count(1)
            FROM item i
            JOIN region r ON i.region_id = r.region_id
            WHERE i.item_type = 'AUCTION'
              AND i.item_status = CAST(:status AS varchar)
              AND (
                   :me IS NULL OR i.seller_id NOT IN (
                   SELECT bl.blocked_user_id
                   FROM block_list bl
                   WHERE bl.blocker_id = :me
                   )
              )
              AND (
                6371 * acos(
                  LEAST(1, GREATEST(-1,
                    cos(radians(:lat)) * cos(radians(CAST(r.latitude AS double precision))) *
                    cos(radians(CAST(r.longitude AS double precision)) - radians(:lon)) +
                    sin(radians(:lat)) * sin(radians(CAST(r.latitude AS double precision)))
                  ))
                )
              ) <= :distanceKm
            """,
            nativeQuery = true)
    Page<Item> findNearbyItems(
            @Param("status") String status,
            @Param("lat") double centerLat,
            @Param("lon") double centerLon,
            @Param("distanceKm") double distanceKm,
            @Param("me") Long me,
            Pageable pageable
    );

    /**
     * 키워드로 제목/이름을 부분일치 검색(JPQL) — 거리 필터 없음.
     */
    @Query("""
            SELECT i FROM Item i
            WHERE i.itemType = com.salemale.global.common.enums.ItemType.AUCTION
              AND i.itemStatus = :status
              AND (
                   :me IS NULL OR i.seller_id NOT IN (
                       SELECT bl.blocked_user_id
                       FROM block_list bl
                       WHERE bl.blocker_id = :me
                   )
               )
              AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY i.createdAt DESC
            """)
    Page<Item> searchItemsByKeyword(
            @Param("status") com.salemale.global.common.enums.ItemStatus status,
            @Param("keyword") String keyword,
            @Param("me") Long me,
            Pageable pageable
    );

    /**
     * 키워드 + 필터(JPQL, 전국검색용)
     * - 상태, 카테고리(옵션), 최소/최대 가격(옵션)을 함께 적용
     * - 정렬은 Pageable의 Sort로 처리
     * - POPULAR 상태인 경우: bidCount >= 3, 3일 이내 생성, 진행중인 상품만
     */
    @Query("""
            SELECT i FROM Item i
            WHERE i.itemType = com.salemale.global.common.enums.ItemType.AUCTION
              AND i.itemStatus = :status
              AND (
                    :me IS NULL OR i.seller_id NOT IN (
                        SELECT bl.blocked_user_id
                        FROM block_list bl
                        WHERE bl.blocker_id = :me
                    )
                )
              AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
              AND (:categories IS NULL OR i.category IN :categories)
              AND (:minPrice IS NULL OR i.currentPrice >= :minPrice)
              AND (:maxPrice IS NULL OR i.currentPrice <= :maxPrice)
              AND (:isPopular = false OR (
                i.bidCount >= 3
                AND i.createdAt >= :threeDaysAgo
                AND i.endTime > :now
              ))
            """)
    Page<Item> searchItemsByKeywordWithFilters(
            @Param("status") com.salemale.global.common.enums.ItemStatus status,
            @Param("keyword") String keyword,
            @Param("categories") java.util.List<com.salemale.global.common.enums.Category> categories,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("isPopular") boolean isPopular,
            @Param("threeDaysAgo") LocalDateTime threeDaysAgo,
            @Param("now") LocalDateTime now,
            @Param("me") Long me,
            Pageable pageable
    );

    /**
     * 필터만 적용 (키워드 없음, JPQL, 전국검색용)
     * - 상태, 카테고리(옵션), 최소/최대 가격(옵션)을 함께 적용
     * - 정렬은 Pageable의 Sort로 처리
     * - POPULAR 상태인 경우: bidCount >= 3, 3일 이내 생성, 진행중인 상품만
     */
    @Query("""
            SELECT i FROM Item i
            WHERE i.itemType = com.salemale.global.common.enums.ItemType.AUCTION
              AND i.itemStatus = :status
              AND (
                   :me IS NULL OR i.seller_id NOT IN (
                       SELECT bl.blocked_user_id
                       FROM block_list bl
                       WHERE bl.blocker_id = :me
                   )
               )
              AND (:categories IS NULL OR i.category IN :categories)
              AND (:minPrice IS NULL OR i.currentPrice >= :minPrice)
              AND (:maxPrice IS NULL OR i.currentPrice <= :maxPrice)
              AND (:isPopular = false OR (
                i.bidCount >= 3
                AND i.createdAt >= :threeDaysAgo
                AND i.endTime > :now
              ))
            """)
    Page<Item> searchItemsByFiltersOnly(
            @Param("status") com.salemale.global.common.enums.ItemStatus status,
            @Param("categories") java.util.List<com.salemale.global.common.enums.Category> categories,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("isPopular") boolean isPopular,
            @Param("threeDaysAgo") LocalDateTime threeDaysAgo,
            @Param("now") LocalDateTime now,
            @Param("me") Long me,
            Pageable pageable
    );

    /**
     * 키워드 + 반경 검색(네이티브, 하버사인)
     */
    @Query(value = """
            SELECT i.*
            FROM item i
            JOIN region r ON i.region_id = r.region_id
            WHERE i.item_type = 'AUCTION'
              AND i.item_status = CAST(:status AS varchar)
              AND (
                    :me IS NULL OR i.seller_id NOT IN (
                        SELECT bl.blocked_user_id
                        FROM block_list bl
                        WHERE bl.blocker_id = :me
                    )
                )
              AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
              AND (
                6371 * acos(
                  LEAST(1, GREATEST(-1,
                    cos(radians(:lat)) * cos(radians(CAST(r.latitude AS double precision))) *
                    cos(radians(CAST(r.longitude AS double precision)) - radians(:lon)) +
                    sin(radians(:lat)) * sin(radians(CAST(r.latitude AS double precision)))
                  ))
                )
              ) <= :distanceKm
            ORDER BY i.created_at DESC
            """,
            countQuery = """
            SELECT count(1)
            FROM item i
            JOIN region r ON i.region_id = r.region_id
            WHERE i.item_type = 'AUCTION'
              AND i.item_status = CAST(:status AS varchar)
              AND (
                   :me IS NULL OR i.seller_id NOT IN (
                   SELECT bl.blocked_user_id
                   FROM block_list bl
                   WHERE bl.blocker_id = :me
                   )
              g)
              AND (
                LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
              AND (
                6371 * acos(
                  LEAST(1, GREATEST(-1,
                    cos(radians(:lat)) * cos(radians(CAST(r.latitude AS double precision))) *
                    cos(radians(CAST(r.longitude AS double precision)) - radians(:lon)) +
                    sin(radians(:lat)) * sin(radians(CAST(r.latitude AS double precision)))
                  ))
                )
              ) <= :distanceKm
            """,
            nativeQuery = true)
    Page<Item> findNearbyItemsByKeyword(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("lat") double centerLat,
            @Param("lon") double centerLon,
            @Param("distanceKm") double distanceKm,
            @Param("me") Long me,
            Pageable pageable
    );
    Long countBySeller(User seller);

    // 낙찰받은 상품 개수 (낙찰 되었으면 상태는 무조건 success임)
    Long countByWinner(User winner);

    /**
     * 내가 낙찰받은 상품 중 특정 상태의 개수
     * WON 탭에서 정확한 개수를 세기 위해 사용
     */
    long countByWinnerAndItemStatus(User winner, ItemStatus itemStatus);

    // 특정 상태의 상품 개수
    Long countBySellerAndItemStatus(User seller, ItemStatus itemStatus);

    /**
     * 여러 ID로 상품 조회 (이미지 fetch join)
     * 추천 상품 조회 시 사용
     * DISTINCT 제거: PostgreSQL JSON 타입은 equality operator가 없어서 DISTINCT 사용 불가
     * 대신 Java에서 중복 제거
     */
    @Query("SELECT i FROM Item i " +
            "LEFT JOIN FETCH i.images " +
            "WHERE i.itemId IN :itemIds")
    List<Item> findAllByItemIdInWithImages(@Param("itemIds") List<Long> itemIds);

    // [알람용 추가] 종료 시간이 남은 입찰중인(BIDDING) 경매 조회
    List<Item> findByEndTimeBetweenAndItemStatus(
            LocalDateTime start,
            LocalDateTime end,
            ItemStatus status
    );
}