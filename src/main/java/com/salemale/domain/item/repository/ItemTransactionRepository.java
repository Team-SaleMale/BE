package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.ItemTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemTransactionRepository extends JpaRepository<ItemTransaction, Long> {

    /**
     * 특정 경매 상품에 입찰이 존재하는지 확인
     */
    boolean existsByItem(Item item);

    /**
     * 특정 경매 상품의 최고 입찰 조회 (금액이 가장 높은 입찰)
     * 동일 금액이면 먼저 입찰한 사람이 우선
     */
    @Query("SELECT it FROM ItemTransaction it WHERE it.item = :item " +
            "ORDER BY it.bidPrice DESC, it.createdAt ASC")
    Optional<ItemTransaction> findTopByItemOrderByBidPriceDescCreatedAtAsc(@Param("item") Item item);
}