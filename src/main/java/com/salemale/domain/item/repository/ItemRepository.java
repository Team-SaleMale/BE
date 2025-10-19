package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.global.common.enums.ItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
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
}