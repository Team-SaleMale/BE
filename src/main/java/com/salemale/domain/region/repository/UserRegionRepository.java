package com.salemale.domain.region.repository;

import com.salemale.domain.region.entity.UserRegion; // UserRegion 엔티티
import com.salemale.domain.user.entity.User; // User 엔티티
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {

    /**
     * 특정 사용자(User)의 대표 지역(isPrimary=true) 매핑 정보를 조회합니다.
     * ItemService에서 대표 지역을 찾기 위해 사용됩니다.
     */
    Optional<UserRegion> findByUserAndIsPrimary(User user, boolean isPrimary);

    // 편의 메서드: findByPrimaryUser(User user)로 ItemService에서 깔끔하게 사용 가능
    default Optional<UserRegion> findByPrimaryUser(User user) {
        return findByUserAndIsPrimary(user, true);
    }
}