package com.salemale.domain.item.repository; // 아이템(상품) 도메인의 리포지토리 계층

import com.salemale.domain.item.entity.Item; // 아이템 엔티티
import org.springframework.data.jpa.repository.JpaRepository; // 기본 CRUD 기능 제공
import org.springframework.stereotype.Repository; // 스프링 빈으로 등록하기 위한 애노테이션

/**
 * 📦 ItemRepository
 * - 상품(Item) 엔티티에 대한 DB 접근 기능을 담당
 * - JpaRepository를 상속하여 findById, save, delete 등 기본 CRUD 메서드를 자동 제공함
 * - 필요 시 커스텀 쿼리 메서드를 추가할 수 있음
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    // 예시: 특정 판매자 ID로 등록된 아이템 조회
    // List<Item> findBySeller_Id(Long sellerId);

    // 예시: 제목에 특정 단어가 포함된 아이템 검색 (부분 일치)
    // List<Item> findByTitleContainingIgnoreCase(String keyword);
}
