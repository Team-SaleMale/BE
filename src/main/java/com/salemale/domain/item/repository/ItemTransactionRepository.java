package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.ItemTransaction;
import com.salemale.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ItemTransactionRepository extends JpaRepository<ItemTransaction, Long> {

    //특정 경매 상품에 입찰이 존재하는지 확인
    boolean existsByItem(Item item);

    //특정 경매 상품의 최고 입찰 조회 (금액이 가장 높은 입찰)
    Optional<ItemTransaction> findTopByItemOrderByBidPriceDescCreatedAtAsc(Item item);

    // 특정 상품의 총 입찰 수를 조회
    Long countByItem(Item item);

    // 특정 사용자가 특정 상품에 입찰했는지 확인
    boolean existsByBuyerAndItem(User buyer, Item item);
}