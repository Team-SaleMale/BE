package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.global.common.enums.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    // 종료 시간이 지났고 아직 상태가 bidding(입찰중)인 상품들 조회
    List<Item> findByEndTimeBeforeAndItemStatus(
            LocalDateTime currentTime,
            ItemStatus status
    );
}