package com.salemale.domain.item.repository;

import com.salemale.domain.item.entity.Item;
import com.salemale.domain.item.entity.UserLiked;
import com.salemale.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLikedRepository extends JpaRepository<UserLiked, Long> {

    // User와 Item으로 찜 레코드 찾기
    Optional<UserLiked> findByUserAndItem(User user, Item item);

    // 찜 여부 확인
    boolean existsByUserAndItem(User user, Item item);

    // 특정 상품에 찜한 총 개수 조회
    Long countByItem(Item item);
}