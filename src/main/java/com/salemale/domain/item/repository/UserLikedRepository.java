package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.UserLiked;
import com.salemale.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserLikedRepository extends JpaRepository<UserLiked, Long> {

    // User와 Item으로 찜 레코드 찾기
    Optional<UserLiked> findByUserAndItem(User user, Item item);

    // 찜 여부 확인
    boolean existsByUserAndItem(User user, Item item);

    // 특정 상품에 찜한 총 개수 조회
    Long countByItem(Item item);

    /**
     * 사용자가 찜한 상품 목록 조회 (페이징)
     * - liked=true인 항목만 조회
     * - 최신 찜한 순으로 고정 정렬
     * - Item, Region, Seller 정보를 Fetch Join으로 한 번에 조회
     *
     * @param user 조회할 사용자
     * @param pageable 페이징 정보
     * @return 찜한 상품 페이지
     */
    @Query("SELECT ul FROM UserLiked ul " +
            "JOIN FETCH ul.item i " +
            "JOIN FETCH i.seller " +
            "JOIN FETCH i.region " +
            "WHERE ul.user = :user AND ul.liked = true " +
            "ORDER BY ul.createdAt DESC")
    Page<UserLiked> findLikedItemsByUser(@Param("user") User user, Pageable pageable);
}