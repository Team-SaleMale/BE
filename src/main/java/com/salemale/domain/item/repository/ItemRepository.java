package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    // 기본 CRUD 메서드만으로 충분
}