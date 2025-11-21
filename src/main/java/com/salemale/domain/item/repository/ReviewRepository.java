package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.Review;
import com.salemale.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 특정 상품에 대해 특정 사용자가 작성한 후기가 있는지 확인
    boolean existsByItemAndReviewer(Item item, User reviewer);

    // 특정 사용자가 받은 모든 후기 조회 (마이페이지 등에서 활용 가능)
    List<Review> findByTarget(User target);

    // 특정 상품의 모든 후기 조회
    List<Review> findByItem(Item item);

    /**
     * 특정 사용자가 받은 후기 목록을 페이징 조회 (최신순)
     * @param target 후기를 받은 사용자
     * @param pageable 페이징 정보
     * @return 페이징된 후기 목록
     */
    Page<Review> findByTargetOrderByCreatedAtDesc(User target, Pageable pageable);
}