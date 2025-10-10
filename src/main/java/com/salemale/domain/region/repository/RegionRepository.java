package com.salemale.domain.region.repository;

import com.salemale.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {
    // 기본 CRUD 메서드만으로 충분
}