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

    /**
     * 사용자가 입찰한 상품 개수 (중복 제거)
     * @param buyer 입찰자
     * @return 입찰한 상품 개수
     */
    @Query("SELECT COUNT(DISTINCT t.item) FROM ItemTransaction t WHERE t.buyer = :buyer")
    Long countDistinctItemByBuyer(@Param("buyer") User buyer);

    /**
     * 사용자가 특정 상품의 최고가 입찰자인지 확인
     * @param item 상품
     * @param buyer 사용자
     * @return 최고가 입찰자 여부
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM ItemTransaction t " +
            "WHERE t.item = :item " +
            "AND t.buyer = :buyer " +
            "AND t.bidPrice = (SELECT MAX(t2.bidPrice) FROM ItemTransaction t2 WHERE t2.item = :item)")
    Boolean isHighestBidder(@Param("item") Item item, @Param("buyer") User buyer);
}