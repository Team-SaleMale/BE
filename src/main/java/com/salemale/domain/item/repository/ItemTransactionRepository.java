package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.ItemTransaction;
import com.salemale.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ItemTransactionRepository extends JpaRepository<ItemTransaction, Long> {

    //특정 경매 상품에 입찰이 존재하는지 확인
    boolean existsByItem(Item item);

    //특정 경매 상품의 최고 입찰 조회 (금액이 가장 높은 입찰)
    Optional<ItemTransaction> findTopByItemOrderByBidPriceDescCreatedAtAsc(Item item);

    // 특정 사용자가 특정 상품에 입찰했는지 확인
    boolean existsByBuyerAndItem(User buyer, Item item);

    /**
     * 특정 상품의 입찰 내역을 최신순으로 조회 (페이징)
     *
     * @param item     상품
     * @param pageable 페이지 정보
     * @return 입찰 내역 리스트
     */
    // fetch join으로 N+1 방지하면서 조회
    @Query("SELECT it FROM ItemTransaction it " +
            "JOIN FETCH it.buyer " +
            "WHERE it.item = :item " +
            "ORDER BY it.createdAt DESC")
    List<ItemTransaction> findBidHistoryByItem(@Param("item") Item item, Pageable pageable);
}