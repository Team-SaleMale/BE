package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.Review;
import com.salemale.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 특정 상품에 대해 특정 사용자가 작성한 후기가 있는지 확인
    boolean existsByItemAndReviewer(Item item, User reviewer);

    // 특정 사용자가 받은 모든 후기 조회 (마이페이지 등에서 활용 가능)
    List<Review> findByTarget(User target);

    // 특정 상품의 모든 후기 조회
    List<Review> findByItem(Item item);
}