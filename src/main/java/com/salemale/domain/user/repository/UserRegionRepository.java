package com.salemale.domain.user.repository; // User-Region 관계 리포지토리

import com.salemale.domain.region.entity.Region;
import com.salemale.domain.user.entity.UserRegion;
import com.salemale.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRegionRepository extends JpaRepository<UserRegion, Long> {
    List<UserRegion> findAllByUser(User user);
    boolean existsByUserAndRegion(User user, Region region);
}
